package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Setting;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.SettingRepository;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {
    private final SettingRepository settingRepository;

    @Override
    public Integer getIntSetting(SettingKey key) {
        Setting setting = settingRepository.findByKey(key);
        return setting == null ? null : Integer.parseInt(setting.getValue());
    }

    @Override
    public long getLongSetting(SettingKey key) {
        String value = settingRepository.findByKey(key).getValue();
        return Long.parseLong(value);
    }

    @Override
    public String getStringSetting(SettingKey key) {
        Setting setting = settingRepository.findByKey(key);
        return setting == null ? null : setting.getValue();
    }

    @Override
    public void saveSetting(SettingKey key, String value) {
        Setting existingSetting = settingRepository.findByKey(key);
        if (existingSetting != null) {
            existingSetting.setValue(value);
            settingRepository.save(existingSetting);
        } else {
            settingRepository.save(new Setting(key, value));
        }
    }
}
