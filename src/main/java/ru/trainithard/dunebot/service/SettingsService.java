package ru.trainithard.dunebot.service;

public interface SettingsService {
    int getIntSetting(String key);

    long getLongSetting(String key);

    String getStringSetting(String key);
}
