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

    REGISTER(true, 2),
    DUNE(false, 1),
    UP4(false, 1),
    UP6(false, 1),
    CANCEL(false, 1),
    SUBMIT(false, 2);

    private static final Map<String, Command> availableCommands;

    private final boolean anonymous;
    private final int wordsCount;

    static {
        availableCommands = Arrays.stream(values()).collect(Collectors.toMap(Command::name, Function.identity()));
    }

    public static Command getCommand(String name) {
        return availableCommands.get(name.toUpperCase());
    }
}
