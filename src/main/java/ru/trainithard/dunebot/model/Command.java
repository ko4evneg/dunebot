package ru.trainithard.dunebot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.trainithard.dunebot.configuration.SettingConstants;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Command {

    REGISTER(true, SettingConstants.STEAM_NAME_MAX_WORDS),
    REFRESH_PROFILE(false, SettingConstants.STEAM_NAME_MAX_WORDS),
    NEW(false, 1),
    VOTE(false, 0),
    CANCEL(false, 0),
    SUBMIT(false, 1);

    private static final Map<String, Command> availableCommands;

    private final boolean anonymous;
    private final int argumentsCount;

    static {
        availableCommands = Arrays.stream(values()).collect(Collectors.toMap(Command::name, Function.identity()));
    }

    public static Command getCommand(String name) {
        return availableCommands.get(name.toUpperCase());
    }
}
