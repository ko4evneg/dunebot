package ru.trainithard.dunebot.util;

import java.util.List;
import java.util.Random;

public class EmojiRandomizer {
    private static final List<String> WINNER_EMOJIS = List
            .of("🥳", "🎉", "🎊", "👑", "🌟", "✨", "💐", "🙌", "💫", "🥂");
    private static final Random random = new Random();

    private EmojiRandomizer() {
    }

    public static String getWinnerEmoji() {
        int emojiIndex = random.nextInt(WINNER_EMOJIS.size());
        return WINNER_EMOJIS.get(emojiIndex);
    }
}
