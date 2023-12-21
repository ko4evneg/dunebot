package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.dto.TelegramUserMessageDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static ru.trainithard.dunebot.configuration.SettingConstants.CHAT_ID;

@Service
@RequiredArgsConstructor
// TODO:  reduce this class?
public class TelegramBotAdapter {
    private final TelegramBot telegramBot;

    private static final List<String> POLL_OPTIONS = List.of("Да", "Нет", "Результат");

    public TelegramUserMessageDto sendPoll(Player initiator, ModType modType) {
        try {
            CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(getNewPoll(initiator, modType));
            TelegramUserMessageDto telegramUserMessage = new TelegramUserMessageDto();
            sendMessageCompletableFuture.whenComplete((message, throwable) -> {
                telegramUserMessage.setTelegramPollId(message.getPoll().getId());
                telegramUserMessage.setTelegramMessageId(message.getMessageId());
                telegramUserMessage.setThrowable(throwable);
            });
            return telegramUserMessage;
        } catch (TelegramApiException exception) {
            throw new TelegramApiCallException("sendPoll() encounters API exception", exception);
        }
    }

    private SendPoll getNewPoll(Player initiator, ModType modType) {
        SendPoll sendPoll = new SendPoll();
        sendPoll.setQuestion("Игрок " + initiator.getFriendlyName() + " призывает всех на матч в " + modType.getModName());
        sendPoll.setAllowMultipleAnswers(false);
        sendPoll.setIsAnonymous(false);
        sendPoll.setChatId(CHAT_ID);
        sendPoll.setMessageThreadId(getTopicId(modType));
        sendPoll.setOptions(POLL_OPTIONS);
        return sendPoll;
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> SettingConstants.TOPIC_ID_CLASSIC;
            case UPRISING_4, UPRISING_6 -> SettingConstants.TOPIC_ID_UPRISING;
        };
    }

    public void deleteMessage(int telegramMessageId, String telegramChatId, BiConsumer<Boolean, Throwable> onCompleteAction) {
        try {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setMessageId(telegramMessageId);
            deleteMessage.setChatId(telegramChatId);

            CompletableFuture<Boolean> deleteMessageCompletableFuture = telegramBot.executeAsync(deleteMessage);
            deleteMessageCompletableFuture.whenComplete(onCompleteAction);
        } catch (TelegramApiException exception) {
            throw new TelegramApiCallException("deleteMessage() encounters API exception", exception);
        }
    }
}
