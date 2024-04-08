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
    public int getIntSetting(SettingKey key) {
        String value = settingRepository.findByKey(key).getValue();
        return Integer.parseInt(value);
    }

    @Override
    public long getLongSetting(SettingKey key) {
        String value = settingRepository.findByKey(key).getValue();
        return Long.parseLong(value);
    }

    @Override
    public String getStringSetting(SettingKey key) {
        String value = settingRepository.findByKey(key).getValue();
        return value;
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
