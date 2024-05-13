package ru.trainithard.dunebot.model;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Setting key for user setting values
 */
@RequiredArgsConstructor
public enum UserSettingKey {
    /**
     * Server credentials for game server hosting
     */
    HOST("Данные сервера Tabletop");

    private final String friendlyName;

    public static UserSettingKey getByName(String settingName) {
        if (settingName == null || settingName.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(settingKey -> settingName.equalsIgnoreCase(settingKey.name()))
                .findFirst().orElse(null);
    }
}
