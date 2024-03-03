package ru.trainithard.dunebot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownEscaperTest {
    @Test
    void shouldEscapeText() {
        String actualText = MarkdownEscaper.getEscaped("a1bfg`h>i#j+k-l=m|n{o}p.q!r");

        assertEquals("a1bfg\\`h\\>i\\#j\\+k\\-l\\=m\\|n\\{o\\}p\\.q\\!r", actualText);
    }

    @Test
    void shouldNotEscapeText() {
        String actualText = MarkdownEscaper.getEscaped("AZabzАЯабя019,\\<'?:;%^&$@");

        assertEquals("AZabzАЯабя019,\\<'?:;%^&$@", actualText);
    }
}
