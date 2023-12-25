package ru.trainithard.dunebot.exception;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.model.Player;

public class AnswerableDubeBotException extends DubeBotException {
    private final long telegramUserId;
    private final long telegramChatId;

    public AnswerableDubeBotException(String message, Message telegramMessage) {
        super(message);
        this.telegramUserId = telegramMessage.getFrom().getId();
        this.telegramChatId = telegramMessage.getChatId();
    }

    public AnswerableDubeBotException(String message, Player player) {
        super(message);
        this.telegramUserId = player.getTelegramId();
        this.telegramChatId = player.getTelegramChatId();
    }
}
