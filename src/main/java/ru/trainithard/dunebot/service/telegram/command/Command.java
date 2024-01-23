package ru.trainithard.dunebot.service.telegram.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.trainithard.dunebot.service.telegram.command.CommandType.*;

/**
 * Enum describing available bot commands.
 */
@Getter
@RequiredArgsConstructor
public enum Command {

    REGISTER(TEXT, true, false, 1),
    REFRESH_PROFILE(TEXT, false, false, 1),
    NEW(TEXT, false, false, 1),
    VOTE(POLL_VOTE, false, true, 0),
    CANCEL(TEXT, false, false, 0),
    SUBMIT(TEXT, false, false, 1),
    RESUBMIT(TEXT, false, false, 1),
    UPLOAD_PHOTO(FILE_UPLOAD, false, false, 0),
    ACCEPT_SUBMIT(CALLBACK, false, false, 0);

    private static final Map<String, Command> availableCommands;

    /**
     * Type of the command.
     */
    private final CommandType commandType;
    /**
     * Whether the command can be invoked by unregistered user.
     */
    private final boolean anonymous;
    /**
     * Whether a public chat could be a source of the command.
     */
    private final boolean publicCommand;
    /**
     * Minimal arguments count required for the command.
     */
    private final int minimalArgumentsCount;

    static {
        availableCommands = Arrays.stream(values()).collect(Collectors.toMap(Command::name, Function.identity()));
    }

    public static Command getCommand(String name) {
        return availableCommands.get(name.toUpperCase());
    }
}