package ru.trainithard.dunebot.service.messaging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.dto.*;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TelegramMessagingService implements MessagingService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramMessagingService.class);
    private static final String SEND_POLL_CALLBACK_EXCEPTION_MESSAGE = "sendPollAsync() call encounters API exception";
    private static final String SEND_MESSAGE_CALLBACK_EXCEPTION_MESSAGE = "sendMessageAsync() call encounters API exception";
    private static final String SEND_DOCUMENT_CALLBACK_EXCEPTION_MESSAGE = "sendDocumentAsync() call encounters API exception";
    private static final String GET_FILE_DETAILS_EXCEPTION_MESSAGE = "getFile() call encounters API exception";
    private static final String SET_COMMANDS_LIST_EXCEPTION_MESSAGE = "sendSetCommands() call encounters API exception";
    private static final String DELETE_MESSAGE_CALLBACK_EXCEPTION_MESSAGE = "deleteMessageAsync() call encounters API exception";
    private static final String MARKDOWN_PARSE_MODE = "Markdown";

    private final TelegramBot telegramBot;

    @Override
    public void deleteMessageAsync(ExternalMessageId externalMessageId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(externalMessageId.getChatIdString(), externalMessageId.getMessageId());
            telegramBot.executeAsync(deleteMessage);
        } catch (TelegramApiException exception) {
            logger.error(DELETE_MESSAGE_CALLBACK_EXCEPTION_MESSAGE, exception);
        }
    }

    @Override
    public CompletableFuture<ExternalPollDto> sendPollAsync(PollMessageDto pollMessage) {
        CompletableFuture<ExternalPollDto> telegramMessageCompletableFuture = new CompletableFuture<>();
        try {
            CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(getSendPoll(pollMessage));
            sendMessageCompletableFuture.whenComplete((message, throwable) -> {
                if (throwable == null) {
                    telegramMessageCompletableFuture.complete(new ExternalPollDto(message));
                } else {
                    telegramMessageCompletableFuture.isCompletedExceptionally();
                    logger.error(SEND_POLL_CALLBACK_EXCEPTION_MESSAGE, throwable);
                }
            });
        } catch (TelegramApiException exception) {
            logger.error(SEND_POLL_CALLBACK_EXCEPTION_MESSAGE, exception);
        }
        return telegramMessageCompletableFuture;
    }

    private SendPoll getSendPoll(PollMessageDto pollMessage) {
        SendPoll sendPoll = new SendPoll(pollMessage.getChatId(), pollMessage.getText(), pollMessage.getOptions());
        sendPoll.setReplyToMessageId(pollMessage.getReplyMessageId());
        sendPoll.setIsAnonymous(false);
        sendPoll.setAllowMultipleAnswers(false);
        return sendPoll;
    }

    @Override
    public CompletableFuture<ExternalMessageDto> sendMessageAsync(MessageDto messageDto) {
        CompletableFuture<ExternalMessageDto> telegramMessageCompletableFuture = new CompletableFuture<>();
        try {
            CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(getSendMessage(messageDto));
            sendMessageCompletableFuture.whenComplete((message, throwable) -> {
                if (throwable == null) {
                    telegramMessageCompletableFuture.complete(new ExternalMessageDto(message));
                } else {
                    telegramMessageCompletableFuture.isCompletedExceptionally();
                    logger.error(SEND_MESSAGE_CALLBACK_EXCEPTION_MESSAGE, throwable);
                }
            });
        } catch (TelegramApiException exception) {
            logger.error(SEND_MESSAGE_CALLBACK_EXCEPTION_MESSAGE, exception);
        }
        return telegramMessageCompletableFuture;
    }

    private SendMessage getSendMessage(MessageDto message) {
        SendMessage sendMessage = new SendMessage(message.getChatId(), message.getText());
        sendMessage.setParseMode(MARKDOWN_PARSE_MODE);
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
                telegramMessageCompletableFuture.isCompletedExceptionally();
                logger.error(SEND_DOCUMENT_CALLBACK_EXCEPTION_MESSAGE, throwable);
            }
        });
        return telegramMessageCompletableFuture;
    }

    private SendDocument getSendDocument(FileMessageDto fileMessageDto) {
        InputStream fileInputStream = new ByteArrayInputStream(fileMessageDto.getFile());
        InputFile inputFile = new InputFile(fileInputStream, fileMessageDto.getFileName());
        SendDocument sendDocument = new SendDocument(fileMessageDto.getChatId(), inputFile);
        sendDocument.setReplyToMessageId(fileMessageDto.getReplyMessageId());
        sendDocument.setCaption(fileMessageDto.getText());
        return sendDocument;
    }

    @Override
    public CompletableFuture<TelegramFileDetailsDto> getFileDetails(String fileId) {
        CompletableFuture<TelegramFileDetailsDto> telegramMessageCompletableFuture = new CompletableFuture<>();
        try {
            GetFile getFile = new GetFile(fileId);
            CompletableFuture<File> fileCompletableFuture = telegramBot.executeAsync(getFile);
            fileCompletableFuture.whenComplete((message, throwable) -> {
                if (throwable == null) {
                    telegramMessageCompletableFuture.complete(new TelegramFileDetailsDto(message));
                } else {
                    telegramMessageCompletableFuture.isCompletedExceptionally();
                    logger.error(GET_FILE_DETAILS_EXCEPTION_MESSAGE, throwable);
                }
            });
        } catch (TelegramApiException exception) {
            logger.error(GET_FILE_DETAILS_EXCEPTION_MESSAGE, exception);
        }
        return telegramMessageCompletableFuture;

    }

    @Override
    public void sendSetCommands(SetCommandsDto setCommandsDto) {
        List<BotCommand> botCommands = setCommandsDto.getCommandDescriptionsByName().entrySet().stream()
                .map(entry -> new BotCommand(entry.getKey(), entry.getValue()))
                .toList();
        SetMyCommands setMyCommands = new SetMyCommands(botCommands, new BotCommandScopeDefault(), null);

        try {
            telegramBot.executeAsync(setMyCommands);
        } catch (TelegramApiException exception) {
            throw new TelegramApiCallException(SET_COMMANDS_LIST_EXCEPTION_MESSAGE, exception);
        }
    }
}
