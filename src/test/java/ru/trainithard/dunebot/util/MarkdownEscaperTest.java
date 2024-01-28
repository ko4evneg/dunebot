package ru.trainithard.dunebot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownEscaperTest {
    @Test
    void shouldEscapeText() {
        String actualText = MarkdownEscaper.getEscaped("a.1!b`c#y-f+");

        assertEquals("a\\.1\\!b\\`c\\#y\\-f\\+", actualText);
    }

    @Test
    void shouldNotEscapeText() {
        String actualText = MarkdownEscaper.getEscaped("a,1~b'c*y_f?");

        assertEquals("a,1~b'c*y_f?", actualText);
    }
}
