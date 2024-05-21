package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.model.AppSettingKey;

public interface AppSettingsService {

    Integer getIntSetting(AppSettingKey key);

    long getLongSetting(AppSettingKey key);

    String getStringSetting(AppSettingKey key);

    void saveSetting(AppSettingKey key, String value);
}
