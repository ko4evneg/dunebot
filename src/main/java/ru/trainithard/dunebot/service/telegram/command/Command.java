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

    /**
     * Deprecated - redirect message
     */
    REGISTER(TEXT, true, false, 0),
    /**
     * Register new player.
     */
    PROFILE(TEXT, true, false, 0),
    /**
     * Deprecated - redirect message
     */
    REFRESH_PROFILE(TEXT, true, false, 0),
    /**
     * Deprecated - redirect message
     */
    NEW(TEXT, false, false, 0),
    /**
     * Creates new classic Dune match gathering poll.
     */
    NEW_DUNE(TEXT, false, false, 0),
    /**
     * Creates new Uprising 4 players Dune match gathering poll.
     */
    NEW_UP4(TEXT, false, false, 0),
    /**
     * Creates new Uprising 6 players Dune match gathering poll.
     */
    NEW_UP6(TEXT, false, false, 0),
    /**
     * Accepts a vote in a match gathering poll.
     */
    VOTE(POLL_VOTE, true, true, 0),
    /**
     * Cancels owned not finished match.
     */
    CANCEL(TEXT, false, false, 0),
    /**
     * Initiate first match results requests.
     */
    SUBMIT(TEXT, false, false, 1),
    /**
     * Resets current results and initiates match results requests.
     */
    RESUBMIT(TEXT, false, false, 1),
    /**
     * Accepts photo upload with match results.
     */
    UPLOAD_PHOTO(FILE_UPLOAD, true, false, 0),
    /**
     * Accepts player's reply to match result requests.
     */
    ACCEPT_SUBMIT(CALLBACK, true, false, 0),
    /**
     * Accepts winner's reply to leader selection request
     */
    LEADER(CALLBACK, true, false, 0),
    /**
     * Shows bot help.
     */
    HELP(TEXT, true, false, 0),
    /**
     * Redirects to <code>HELP</code> command.
     */
    START(TEXT, true, false, 0),
    /**
     * Set user configuration setting.
     */
    CONFIG(TEXT, false, false, 1),
    /**
     * Manage and configure bot.
     */
    ADMIN(TEXT, true, true, 1);

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
