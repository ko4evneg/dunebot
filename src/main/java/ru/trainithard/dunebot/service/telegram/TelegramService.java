package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
// TODO:  reduce this class?
public class TelegramService {
    private final TelegramBot telegramBot;

    public void sendPoll(SendPoll sendPoll, BiConsumer<Message, Throwable> onCompleteAction) throws TelegramApiException {
        CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(sendPoll);
        sendMessageCompletableFuture.whenComplete(onCompleteAction);
    }

    public void deleteMessage(DeleteMessage deleteMessage, BiConsumer<Boolean, Throwable> onCompleteAction) throws TelegramApiException {
        CompletableFuture<Boolean> deleteMessageCompletableFuture = telegramBot.executeAsync(deleteMessage);
        deleteMessageCompletableFuture.whenComplete(onCompleteAction);
    }
}
