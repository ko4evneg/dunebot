package ru.trainithard.dunebot.configuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SettingConstants {
    public static final String EXTERNAL_LINE_SEPARATOR = "\n";
    public static final int MAX_SCREENSHOT_SIZE = 6_500_000;
    public static final Set<String> PHOTO_ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg");
    public static final int NOT_PARTICIPATED_MATCH_PLACE = 0;
}
