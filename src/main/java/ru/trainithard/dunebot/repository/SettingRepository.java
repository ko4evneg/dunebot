package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.Setting;
import ru.trainithard.dunebot.model.SettingKey;

public interface SettingRepository extends JpaRepository<Setting, Long> {
    Setting findByKey(SettingKey key);
}
