package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Setting;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.SettingRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {
    private final SettingRepository settingRepository;
    //todo replace with L2 cache
    private final Map<SettingKey, String> simpleCache = new ConcurrentHashMap<>();

    @Override
    public int getIntSetting(SettingKey key) {
        String cachedSetting = simpleCache.get(key);
        if (cachedSetting != null) {
            return Integer.parseInt(cachedSetting);
        }
        String value = settingRepository.findByKey(key).getValue();
        simpleCache.put(key, value);
        return Integer.parseInt(value);
    }

    @Override
    public long getLongSetting(SettingKey key) {
        String cachedSetting = simpleCache.get(key);
        if (cachedSetting != null) {
            return Long.parseLong(cachedSetting);
        }
        String value = settingRepository.findByKey(key).getValue();
        simpleCache.put(key, value);
        return Long.parseLong(value);
    }

    @Override
    public String getStringSetting(SettingKey key) {
        String cachedSetting = simpleCache.get(key);
        if (cachedSetting != null) {
            return cachedSetting;
        }
        String value = settingRepository.findByKey(key).getValue();
        simpleCache.put(key, value);
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
        simpleCache.put(key, value);
    }
}
