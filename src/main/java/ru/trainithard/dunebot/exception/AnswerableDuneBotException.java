package ru.trainithard.dunebot.exception;

import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class AnswerableDuneBotException extends DubeBotException {
    private final long telegramChatId;
    private final Integer telegramTopicId;

    public AnswerableDuneBotException(String message, long telegramChatId) {
        super(message);
        this.telegramChatId = telegramChatId;
        this.telegramTopicId = null;
    }

    public AnswerableDuneBotException(String message, long telegramChatId, @Nullable Integer telegramTopicId) {
        super(message);
        this.telegramChatId = telegramChatId;
        this.telegramTopicId = telegramTopicId;
    }
}
