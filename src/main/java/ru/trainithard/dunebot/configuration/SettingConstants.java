package ru.trainithard.dunebot.configuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SettingConstants {
    // TODO:  replace to prod
    public static final String ADMIN_USER_ID = "193506662";
    public static final String CHAT_ID = "-1002139068680";
    public static final int TOPIC_ID_CLASSIC = 2;
    public static final int TOPIC_ID_UPRISING = 4;
    // TODO:  extract
    public static final int POSITIVE_POLL_OPTION_ID = 0;
    public static final int RESUBMITS_LIMIT = 3;
    public static final int MATCH_START_DELAY = 60;
    public static final String EXTERNAL_LINE_SEPARATOR = "\n";
    public static final int FINISH_MATCH_TIMEOUT = 120;
}
