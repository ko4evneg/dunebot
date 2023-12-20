package ru.trainithard.dunebot.exception;

public class TelegramApiCallException extends RuntimeException {
    public TelegramApiCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
