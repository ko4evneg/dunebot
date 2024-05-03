package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.model.SettingKey;

public interface SettingsService {

    Integer getIntSetting(SettingKey key);

    long getLongSetting(SettingKey key);

    String getStringSetting(SettingKey key);

    void saveSetting(SettingKey key, String value);
}
