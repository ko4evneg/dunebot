package ru.trainithard.dunebot.exception;

public class TelegramRetryException extends RuntimeException {
    private static final String RETRY_EXCEPTION = "Reached maximum telegram retry limit";

    public TelegramRetryException() {
        super(RETRY_EXCEPTION);
    }
}
