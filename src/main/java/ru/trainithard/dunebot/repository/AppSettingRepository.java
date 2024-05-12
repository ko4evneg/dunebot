package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.AppSetting;
import ru.trainithard.dunebot.model.AppSettingKey;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    AppSetting findByKey(AppSettingKey key);
}
