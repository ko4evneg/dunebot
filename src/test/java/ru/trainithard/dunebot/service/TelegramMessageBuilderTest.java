package ru.trainithard.dunebot.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelegramMessageBuilderTest {
    @ParameterizedTest
    @ValueSource(strings = {"123", "abc", "abc def", "408\\asda;"})
    void shouldReturnNotEscapableText(String text) {
        TelegramMessageBuilder builder = new TelegramMessageBuilder();
        builder.append(text);

        assertEquals(text, builder.build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abc", "abc def", "408\\asda;"})
    void shouldReturnNotEscapableBoldText(String text) {
        TelegramMessageBuilder builder = new TelegramMessageBuilder();
        builder.appendBold(text);

        assertEquals("*" + text + "*", builder.build());
    }

    @ParameterizedTest
    @MethodSource("escapedTextSource")
    void shouldReturnEscapedText(String text, String expectedText) {
        TelegramMessageBuilder builder = new TelegramMessageBuilder();
        builder.append(text);

        assertEquals(expectedText, builder.build());
    }

    @ParameterizedTest
    @MethodSource("escapedTextSource")
    void shouldReturnEscapedBoldText(String text, String expectedText) {
        TelegramMessageBuilder builder = new TelegramMessageBuilder();
        builder.appendBold(text);

        assertEquals("*" + expectedText + "*", builder.build());
    }

    private static Stream<Arguments> escapedTextSource() {
        return Stream.of(
                Arguments.of("123.!", "123\\.\\!"),
                Arguments.of("a-b+c_d*", "a\\-b\\+c\\_d\\*"),
                Arguments.of("a (bc) [d] {e}", "a \\(bc\\) \\[d\\] \\{e\\}"),
                Arguments.of("~e>f`s=y|p", "\\~e\\>f\\`s\\=y\\|p"),
                Arguments.of("just;plain:text", "just;plain:text")
        );
    }
}
