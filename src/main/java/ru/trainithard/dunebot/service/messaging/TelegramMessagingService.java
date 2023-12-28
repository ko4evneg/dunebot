package ru.trainithard.dunebot.service.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;
import ru.trainithard.dunebot.service.messaging.dto.*;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TelegramMessagingService implements MessagingService {
    private final TelegramBot telegramBot;

    @Override
    public CompletableFuture<ExternalPollDto> sendPollAsync(PollMessageDto pollMessage) {
        CompletableFuture<ExternalPollDto> telegramMessageCompletableFuture = new CompletableFuture<>();
        try {
            CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(getSendPoll(pollMessage));
            sendMessageCompletableFuture.whenComplete((message, throwable) -> {
                if (throwable != null) {
                    throw new TelegramApiCallException("sendPollAsync() call encounters API exception", throwable);
                }
                telegramMessageCompletableFuture.complete(new ExternalPollDto(message));
            });
        } catch (TelegramApiException exception) {
            throw new TelegramApiCallException("sendPollAsync() call encounters API exception", exception);
        }
        return telegramMessageCompletableFuture;
    }

    private SendPoll getSendPoll(PollMessageDto pollMessage) {
        SendPoll sendPoll = new SendPoll(pollMessage.getChatId(), pollMessage.getText(), pollMessage.getOptions());
        sendPoll.setReplyToMessageId(pollMessage.getReplyMessageId());
        sendPoll.setIsAnonymous(pollMessage.isAnonymous());
        sendPoll.setAllowMultipleAnswers(pollMessage.isAllowMultipleAnswers());
        return sendPoll;
    }

    @Override
    public CompletableFuture<ExternalMessageDto> sendMessageAsync(MessageDto messageDto) {
        CompletableFuture<ExternalMessageDto> telegramMessageCompletableFuture = new CompletableFuture<>();
        try {
            CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(getSendMessage(messageDto));
            sendMessageCompletableFuture.whenComplete((message, throwable) -> {
                if (throwable != null) {
                    throw new TelegramApiCallException("sendMessageAsync() call encounters API exception", throwable);
                }
                telegramMessageCompletableFuture.complete(new ExternalMessageDto(message));
            });
        } catch (TelegramApiException exception) {
            throw new TelegramApiCallException("sendMessageAsync() call encounters API exception", exception);
        }
        return telegramMessageCompletableFuture;
    }

    private SendMessage getSendMessage(MessageDto message) {
        SendMessage sendMessage = new SendMessage(message.getChatId(), message.getText());
        sendMessage.setReplyToMessageId(message.getReplyMessageId());
        if (message.getKeyboard() != null) {
            List<List<InlineKeyboardButton>> inlineKeyboard = message.getKeyboard().stream()
                    .map(buttonsRow -> buttonsRow.stream().map(TelegramMessagingService::getInlineKeyboardButton).toList())
                    .toList();
            sendMessage.setReplyMarkup(new InlineKeyboardMarkup(inlineKeyboard));
        }
        return sendMessage;
    }

    private static InlineKeyboardButton getInlineKeyboardButton(ButtonDto button) {
        InlineKeyboardButton inlineButton = new InlineKeyboardButton(button.getText());
        inlineButton.setCallbackData(button.getCallback());
        return inlineButton;
    }

    @Override
    public void deletePollAsync(ExternalPollId externalPollId) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage(Long.toString(externalPollId.getChatId()), externalPollId.getMessageId());
            telegramBot.executeAsync(deleteMessage);
        } catch (TelegramApiException exception) {
            throw new TelegramApiCallException("deletePollAsync() call encounters API exception", exception);
        }
    }
}
