package ru.trainithard.dunebot.exception;

public class MatchNotExistsException extends DuneBotException {
    public static final String MATCH_NOT_EXISTS_EXCEPTION = "Матча с таким ID не существует!";

    public MatchNotExistsException() {
        super("Матч не существует");
    }
}
