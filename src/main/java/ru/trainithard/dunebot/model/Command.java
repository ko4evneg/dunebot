package ru.trainithard.dunebot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum Command {

    REGISTER(true), DUNE(false), UP4(false), UP6(false), CANCEL(false), SUBMIT(false);

    private static final Map<String, Command> availableCommands;

    private final boolean anonymous;

    static {
        availableCommands = Arrays.stream(values()).collect(Collectors.toMap(Command::name, Function.identity()));
    }

    public static Command getCommand(String name) {
        return availableCommands.get(name.toUpperCase());
    }
}
