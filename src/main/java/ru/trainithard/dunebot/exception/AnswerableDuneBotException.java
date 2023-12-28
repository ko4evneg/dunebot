package ru.trainithard.dunebot.exception;

import lombok.Getter;

@Getter
public class AnswerableDuneBotException extends DubeBotException {
    private final long telegramChatId;

    public AnswerableDuneBotException(String message, long telegramChatId) {
        super(message);
        this.telegramChatId = telegramChatId;
    }
}