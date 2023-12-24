package ru.trainithard.dunebot.exception;

public class DubeBotException extends RuntimeException {
    public DubeBotException(Throwable cause) {
        super(cause);
    }

    public DubeBotException(String message) {
        super(message);
    }
}
