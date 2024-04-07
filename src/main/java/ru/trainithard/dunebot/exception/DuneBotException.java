package ru.trainithard.dunebot.exception;

public class DuneBotException extends RuntimeException {

    public DuneBotException(String message) {
        super(message);
    }

    public DuneBotException(String message, Throwable cause) {
        super(message, cause);
    }
}
