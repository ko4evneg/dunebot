package ru.trainithard.dunebot.util;

import lombok.Getter;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.WrongNamesInputException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class ParsedNames {
    private static final String WRONG_INPUT_EXCEPTION_TEXT =
            "Неверный формат ввода имен. Пример верного формата:" + SettingConstants.EXTERNAL_LINE_SEPARATOR +
            "/profile Иван (Лось) Петров";

    private final String firstName;
    private final String lastName;
    private final String steamName;

    public ParsedNames(String commandArguments) throws WrongNamesInputException {
        Pattern pattern = Pattern.compile("([a-zA-Zа-яА-Я]+)\\s+\\(([^()]+)\\)\\s+([a-zA-Zа-яА-Я]+)");
        Matcher matcher = pattern.matcher(commandArguments);
        if (!matcher.matches()) {
            throw new WrongNamesInputException(WRONG_INPUT_EXCEPTION_TEXT);
        }
        this.firstName = matcher.group(1);
        this.steamName = matcher.group(2);
        this.lastName = matcher.group(3);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %s", firstName, steamName, lastName);
    }
}
