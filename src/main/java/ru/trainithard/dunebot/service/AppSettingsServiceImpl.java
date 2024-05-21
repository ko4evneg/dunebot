package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.trainithard.dunebot.model.AppSetting;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.repository.AppSettingRepository;

@Service
@RequiredArgsConstructor
public class AppSettingsServiceImpl implements AppSettingsService {
    private final AppSettingRepository appSettingRepository;

    @Override
    @Cacheable(value = "settings", key = "#key")
    public Integer getIntSetting(AppSettingKey key) {
        AppSetting appSetting = appSettingRepository.findByKey(key);
        return appSetting == null ? null : Integer.parseInt(appSetting.getValue());
    }

    @Override
    @Cacheable(value = "settings", key = "#key")
    public long getLongSetting(AppSettingKey key) {
        String value = appSettingRepository.findByKey(key).getValue();
        return Long.parseLong(value);
    }

    @Override
    @Cacheable(value = "settings", key = "#key")
    public String getStringSetting(AppSettingKey key) {
        AppSetting appSetting = appSettingRepository.findByKey(key);
        return appSetting == null ? null : appSetting.getValue();
    }

    @Override
    @CacheEvict(value = "settings", key = "#key")
    @Transactional
    public void saveSetting(AppSettingKey key, String value) {
        AppSetting existingAppSetting = appSettingRepository.findByKey(key);
        if (existingAppSetting != null) {
            existingAppSetting.setValue(value);
            appSettingRepository.save(existingAppSetting);
        } else {
            appSettingRepository.save(new AppSetting(key, value));
        }
    }
}
