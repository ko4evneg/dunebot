package ru.trainithard.dunebot.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Command {

    REGISTER, DUNE, UP4, UP6, CANCEL, SUBMIT;

    private static final Map<String, Command> availableCommands;

    static {
        availableCommands = Arrays.stream(values()).collect(Collectors.toMap(Command::name, Function.identity()));
    }

    public static Command getCommand(String name) {
        return availableCommands.get(name.toUpperCase());
    }
}
