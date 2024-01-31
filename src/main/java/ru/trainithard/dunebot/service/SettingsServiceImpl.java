package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Setting;
import ru.trainithard.dunebot.repository.SettingRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {
    private final SettingRepository settingRepository;
    //todo replace with L2 cache
    private final Map<String, String> simpleCache = new ConcurrentHashMap<>();

    @Override
    public int getIntSetting(String key) {
        String cachedSetting = simpleCache.get(key.toLowerCase());
        if (cachedSetting != null) {
            return Integer.parseInt(cachedSetting);
        }
        String value = settingRepository.findByKeyIgnoreCase(key).getValue();
        simpleCache.put(key.toLowerCase(), value);
        return Integer.parseInt(value);
    }

    @Override
    public long getLongSetting(String key) {
        String cachedSetting = simpleCache.get(key.toLowerCase());
        if (cachedSetting != null) {
            return Long.parseLong(cachedSetting);
        }
        String value = settingRepository.findByKeyIgnoreCase(key).getValue();
        simpleCache.put(key.toLowerCase(), value);
        return Long.parseLong(value);
    }

    @Override
    public String getStringSetting(String key) {
        String cachedSetting = simpleCache.get(key.toLowerCase());
        if (cachedSetting != null) {
            return cachedSetting;
        }
        String value = settingRepository.findByKeyIgnoreCase(key).getValue();
        simpleCache.put(key.toLowerCase(), value);
        return value;
    }

    @Override
    public void saveSetting(String key, String value) {
        Setting existingSetting = settingRepository.findByKeyIgnoreCase(key);
        if (existingSetting != null) {
            existingSetting.setValue(value);
            settingRepository.save(existingSetting);
        } else {
            settingRepository.save(new Setting(key, value));
        }
        simpleCache.put(key.toLowerCase(), value);
    }
}
