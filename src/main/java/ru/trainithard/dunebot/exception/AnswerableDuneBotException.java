package ru.trainithard.dunebot.exception;

import lombok.Getter;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Getter
public class AnswerableDuneBotException extends DuneBotException {
    private final long telegramChatId;
    private final Integer telegramReplyId;

    public AnswerableDuneBotException(String message, long telegramChatId) {
        super(message);
        this.telegramChatId = telegramChatId;
        this.telegramReplyId = null;
    }

    public AnswerableDuneBotException(String message, CommandMessage commandMessage) {
        super(message);
        this.telegramChatId = commandMessage.getChatId();
        this.telegramReplyId = commandMessage.getReplyMessageId();
    }
}
