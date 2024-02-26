package ru.trainithard.dunebot.service;

public interface SettingsService {
    String ADMIN_USER_ID_KEY = "ADMIN_USER_ID";
    String CHAT_ID_KEY = "CHAT_ID";
    String TOPIC_ID_UPRISING_KEY = "TOPIC_ID_UPRISING";
    String TOPIC_ID_CLASSIC_KEY = "TOPIC_ID_CLASSIC";
    String MATCH_START_DELAY_KEY = "MATCH_START_DELAY";
    String FINISH_MATCH_TIMEOUT_KEY = "FINISH_MATCH_TIMEOUT";
    String RESUBMITS_LIMIT_KEY = "RESUBMITS_LIMIT";
    String MONTHLY_MATCHES_THRESHOLD = "MONTHLY_MATCHES_THRESHOLD";

    int getIntSetting(String key);

    long getLongSetting(String key);

    String getStringSetting(String key);

    void saveSetting(String key, String value);
}
