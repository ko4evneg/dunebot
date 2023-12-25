package ru.trainithard.dunebot.exception;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.model.Player;

@Getter
public class AnswerableDubeBotException extends DubeBotException {
    // TODO:  remove?
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
