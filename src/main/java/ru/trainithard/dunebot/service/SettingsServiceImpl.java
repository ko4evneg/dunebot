package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.repository.SettingRepository;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {
    private final SettingRepository settingRepository;

    @Override
    public int getIntSetting(String key) {
        return Integer.parseInt(settingRepository.findByKeyIgnoreCase(key).getValue());
    }

    @Override
    public long getLongSetting(String key) {
        return Long.parseLong(settingRepository.findByKeyIgnoreCase(key).getValue());
    }

    @Override
    public String getStringSetting(String key) {
        return settingRepository.findByKeyIgnoreCase(key).getValue();
    }
}
