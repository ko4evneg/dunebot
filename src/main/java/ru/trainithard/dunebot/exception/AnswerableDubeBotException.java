package ru.trainithard.dunebot.exception;

import lombok.Getter;

@Getter
public class AnswerableDubeBotException extends DubeBotException {
    private final long telegramChatId;

    public AnswerableDubeBotException(String message, long telegramChatId) {
        super(message);
        this.telegramChatId = telegramChatId;
    }
}
