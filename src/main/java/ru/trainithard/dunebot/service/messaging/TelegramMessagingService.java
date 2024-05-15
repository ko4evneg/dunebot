package ru.trainithard.dunebot.service.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.exception.TelegramRetryException;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.dto.*;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramMessagingService implements MessagingService {
    private static final String ASYNC_MESSAGE_CALLBACK_EXCEPTION_MESSAGE = "asyncRetrySend() call encounters API exception. Chat id: ";
    private static final String SEND_DOCUMENT_CALLBACK_EXCEPTION_MESSAGE = "sendDocumentAsync() call encounters API exception";
    private static final String SET_COMMANDS_LIST_EXCEPTION_MESSAGE = "sendSetCommands() call encounters API exception";
    private static final String DELETE_MESSAGE_CALLBACK_EXCEPTION_MESSAGE = "deleteMessageAsync() call encounters API exception";
    private static final String MARKDOWN2_PARSE_MODE = "MarkdownV2";
    private static final int MAX_RETRY = 3;
    private static final Random random = new Random();
    private final Map<Integer, Long> retryDelayByTryNumber = Map.of(
            1, 1000L,
            2, 2000L,
            3, 3000L,
            4, 5000L);

    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(2);
    private final TelegramBot telegramBot;

    @Override
    public void deleteMessageAsync(ExternalMessageId externalMessageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(externalMessageId.getChatIdString(), externalMessageId.getMessageId());
            telegramBot.executeAsync(deleteMessage);
        } catch (TelegramApiException exception) {
            log.error(DELETE_MESSAGE_CALLBACK_EXCEPTION_MESSAGE, exception);
        }
    }

    @Override
    public CompletableFuture<ExternalPollDto> sendPollAsync(PollMessageDto pollMessage) {
        CompletableFuture<ExternalPollDto> requestFuture = new CompletableFuture<>();
        executeRetryAsync(getSendPoll(pollMessage), requestFuture, ExternalPollDto::new, 0, random.nextLong());
        return requestFuture;
    }

    private SendPoll getSendPoll(PollMessageDto pollMessage) {
        SendPoll sendPoll = new SendPoll(pollMessage.getChatId(), pollMessage.getText(), pollMessage.getOptions());
        sendPoll.setReplyToMessageId(pollMessage.getTopicId());
        sendPoll.setIsAnonymous(false);
        sendPoll.setAllowMultipleAnswers(false);
        return sendPoll;
    }

    @Override
    public CompletableFuture<ExternalMessageDto> sendMessageAsync(MessageDto messageDto) {
        CompletableFuture<ExternalMessageDto> requestFuture = new CompletableFuture<>();
        executeRetryAsync(getSendMessage(messageDto), requestFuture, ExternalMessageDto::new, 0, random.nextLong());
        return requestFuture;
    }

    private SendMessage getSendMessage(MessageDto message) {
        SendMessage sendMessage = new SendMessage(message.getChatId(), message.getText());
        sendMessage.setParseMode(MARKDOWN2_PARSE_MODE);
        sendMessage.setDisableWebPagePreview(true);
        sendMessage.setMessageThreadId(message.getTopicId());
        sendMessage.setReplyToMessageId(message.getReplyMessageId());
        if (message.getKeyboard() != null) {
            sendMessage.setReplyMarkup(getInlineKeyboard(message));
        }
        return sendMessage;
    }

    private InlineKeyboardMarkup getInlineKeyboard(MessageDto message) {
        List<List<InlineKeyboardButton>> inlineKeyboard = message.getKeyboard().stream()
                .map(buttonsRow -> buttonsRow.stream().map(this::getInlineKeyboardButton).toList())
                .toList();
        return new InlineKeyboardMarkup(inlineKeyboard);
    }

    private InlineKeyboardButton getInlineKeyboardButton(ButtonDto button) {
        InlineKeyboardButton inlineButton = new InlineKeyboardButton(button.getText());
        inlineButton.setCallbackData(button.getCallback());
        return inlineButton;
    }

    @Override
    public CompletableFuture<ExternalMessageDto> sendFileAsync(FileMessageDto fileMessage) {
        CompletableFuture<ExternalMessageDto> telegramMessageCompletableFuture = new CompletableFuture<>();
        CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(getSendDocument(fileMessage));
        sendMessageCompletableFuture.whenComplete((message, throwable) -> {
            if (throwable == null) {
                telegramMessageCompletableFuture.complete(new ExternalMessageDto(message));
            } else {
                telegramMessageCompletableFuture.completeExceptionally(throwable);
                log.error(SEND_DOCUMENT_CALLBACK_EXCEPTION_MESSAGE, throwable);
            }
        });
        return telegramMessageCompletableFuture;
    }

    private SendDocument getSendDocument(FileMessageDto fileMessageDto) {
        InputStream fileInputStream = new ByteArrayInputStream(fileMessageDto.getFile());
        InputFile inputFile = new InputFile(fileInputStream, fileMessageDto.getFileName());
        SendDocument sendDocument = new SendDocument(fileMessageDto.getChatId(), inputFile);
        sendDocument.setReplyToMessageId(fileMessageDto.getTopicId());
        sendDocument.setCaption(fileMessageDto.getText());
        sendDocument.setParseMode(MARKDOWN2_PARSE_MODE);
        return sendDocument;
    }

    @Override
    public CompletableFuture<TelegramFileDetailsDto> getFileDetails(String fileId) {
        CompletableFuture<TelegramFileDetailsDto> requestFuture = new CompletableFuture<>();
        executeRetryAsync(new GetFile(fileId), requestFuture, TelegramFileDetailsDto::new, 0, random.nextLong());
        return requestFuture;

    }

    @Override
    public void sendSetCommands(SetCommandsDto setCommandsDto) {
        try {
            List<BotCommand> botCommands = setCommandsDto.getCommandDescriptionsByName().entrySet().stream()
                    .map(entry -> new BotCommand(entry.getKey(), entry.getValue()))
                    .toList();
            SetMyCommands setMyCommands = new SetMyCommands(botCommands, new BotCommandScopeDefault(), null);
            telegramBot.executeAsync(setMyCommands);
        } catch (TelegramApiException exception) {
            log.error(SET_COMMANDS_LIST_EXCEPTION_MESSAGE, exception);
        }
    }

    public <R, T extends Serializable, Method extends BotApiMethod<T>>
    void executeRetryAsync(Method method, CompletableFuture<R> requestFuture, Function<T, R> factory, int retry, long logId) {
        if (retry > MAX_RETRY) {
            requestFuture.completeExceptionally(new TelegramRetryException());
        }

        Long retryDelay = retryDelayByTryNumber.get(retry + 1);
        try {
            CompletableFuture<T> retryFuture = telegramBot.executeAsync(method);
            retryFuture.whenComplete((message, throwable) -> {
                if (throwable == null) {
                    requestFuture.complete(factory.apply(message));
                    log.debug("{}: successful callback received", logId);
                } else {
                    rescheduleIfNeeded(method, requestFuture, factory, retry, logId, throwable, retryDelay);
                }
            });
        } catch (TelegramApiException e) {
            rescheduleIfNeeded(method, requestFuture, factory, retry, logId, e, retryDelay);
        }
    }

    private <R, T extends Serializable, Method extends BotApiMethod<T>> void rescheduleIfNeeded(
            Method method, CompletableFuture<R> requestFuture, Function<T, R> factory,
            int retry, long logId, Throwable exception, Long retryDelay) {
        if (retry > MAX_RETRY) {
            requestFuture.completeExceptionally(exception);
            log.error(logId + ": " + ASYNC_MESSAGE_CALLBACK_EXCEPTION_MESSAGE, exception);
        } else {
            executorService.schedule(() -> executeRetryAsync(method, requestFuture, factory, retry + 1, logId),
                    retryDelay, TimeUnit.MILLISECONDS);
            log.error(logId + ": " + ASYNC_MESSAGE_CALLBACK_EXCEPTION_MESSAGE, exception);
        }
    }
}
