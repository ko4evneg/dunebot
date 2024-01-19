package ru.trainithard.dunebot.exception;

public class MatchNotExistsException extends DuneBotException {
    public MatchNotExistsException() {
        super("Матч не существует");
    }
}
