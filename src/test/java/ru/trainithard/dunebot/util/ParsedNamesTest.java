package ru.trainithard.dunebot.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.exception.WrongNamesInputException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ParsedNamesTest {
    private static final String WRONG_INPUT_EXCEPTION_TEXT = "Неверный формат ввода имен. Пример верного формата:" +
                                                             TestConstants.EXTERNAL_LINE_SEPARATOR + "/register Иван (Лось) Петров" +
                                                             TestConstants.EXTERNAL_LINE_SEPARATOR + "/refresh_profile Иван (Лось) Петров";

    @ParameterizedTest
    @MethodSource("validInputsSource")
    void shouldParseValidInputs(String input, String[] names) throws WrongNamesInputException {
        ParsedNames parsedNames = new ParsedNames(input);

        assertThat(parsedNames)
                .extracting(ParsedNames::getFirstName, ParsedNames::getSteamName, ParsedNames::getLastName)
                .containsExactly(names);
    }

    private static Stream<Arguments> validInputsSource() {
        return Stream.of(
                Arguments.of("Василий (стеам) Петров", new String[]{"Василий", "стеам", "Петров"}),
                Arguments.of("Константин (steam23) Семенов", new String[]{"Константин", "steam23", "Семенов"}),
                Arguments.of("Василий (st EAM 44 !!) Петров", new String[]{"Василий", "st EAM 44 !!", "Петров"}),
                Arguments.of("Василий (st:ea_mm) Петров", new String[]{"Василий", "st:ea_mm", "Петров"}),
                Arguments.of("Василий     (st:ea_mm)           Петров", new String[]{"Василий", "st:ea_mm", "Петров"}),
                Arguments.of("Василий (st:e    a_mm) Петров", new String[]{"Василий", "st:e    a_mm", "Петров"})
        );
    }

    @ParameterizedTest
    @MethodSource("invalidInputsSource")
    void shouldThrowOnInvalidInputs(String input) {
        assertThatCode(() -> new ParsedNames(input))
                .isInstanceOf(WrongNamesInputException.class)
                .hasMessage(WRONG_INPUT_EXCEPTION_TEXT);
    }

    private static Stream<Arguments> invalidInputsSource() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("АБ"),
                Arguments.of("АБ ВГ"),
                Arguments.of("АБ ВГ ДЕ"),
                Arguments.of("(АБ) ВГ ДЕ"),
                Arguments.of("АБ ВГ (ДЕ)"),
                Arguments.of("АБ (ВГ ДЕ"),
                Arguments.of("АБ (ВГ ДЕ)"),
                Arguments.of("(АБ ВГ) ДЕ"),
                Arguments.of("АБ () ДЕ"),
                Arguments.of("ds_dd (ВГ) ДЕ"),
                Arguments.of("АБ (ВГ) ds_dd"),
                Arguments.of("ds_dd (ВГ) ДЕ"),
                Arguments.of("а1 (ВГ) ДЕ"),
                Arguments.of("АБ (ВГ) д1"),
                Arguments.of("А Б (ВГ) ДЕ"),
                Arguments.of("АБ (ВГ) Д Е"),
                Arguments.of("АБ (ВГ)) ДЕ"),
                Arguments.of("АБ(ВГ)ДЕ"),
                Arguments.of("АБ (ВГ)2 Д"),
                Arguments.of("АБ D(ВГ) Д")
        );
    }
}
