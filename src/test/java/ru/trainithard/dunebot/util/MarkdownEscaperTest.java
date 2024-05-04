package ru.trainithard.dunebot.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownEscaperTest {
    @Test
    void shouldEscapeText() {
        String actualText = MarkdownEscaper.getEscaped("a1bfg`h>i#j+k-l=m|n{o}p.q!r");

        assertThat(actualText).isEqualTo("a1bfg\\`h\\>i\\#j\\+k\\-l\\=m\\|n\\{o\\}p\\.q\\!r");
    }

    @Test
    void shouldNotEscapeText() {
        String actualText = MarkdownEscaper.getEscaped("AZabzАЯабя019,\\<'?:;%^&$@");

        assertThat(actualText).isEqualTo("AZabzАЯабя019,\\<'?:;%^&$@");
    }

    @Test
    void shouldEscapeMentionTag() {
        String actualText = MarkdownEscaper.getEscapedMention("a1bfg`h>i#j+k-l=m|n{o}p.q!r", 11111);

        assertThat(actualText).isEqualTo("[@a1bfg\\`h\\>i\\#j\\+k\\-l\\=m\\|n\\{o\\}p\\.q\\!r](tg://user?id=11111)");
    }

    @Test
    void shouldNotEscapeMentionTag() {
        String actualText = MarkdownEscaper.getEscapedMention("AZabzАЯабя019,\\<'?:;%^&$@", 11111);

        assertThat(actualText).isEqualTo("[@AZabzАЯабя019,\\<'?:;%^&$@](tg://user?id=11111)");
    }
}
