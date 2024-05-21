package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.model.UserSetting;
import ru.trainithard.dunebot.model.UserSettingKey;

import java.util.List;
import java.util.Optional;

public interface UserSettingsService {

    Optional<UserSetting> getSetting(long playerId, UserSettingKey key);

    List<UserSetting> getAllSettings(long playerId);

    void saveSetting(long playerId, UserSettingKey key, String value);
}
