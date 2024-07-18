package ru.trainithard.dunebot.util;

import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThatCode;

class EmojiRandomizerTest {
    @RepeatedTest(200)
    void shouldNotThrowOutOfBound() {
        assertThatCode(EmojiRandomizer::getWinnerEmoji)
                .doesNotThrowAnyException();
    }
}
