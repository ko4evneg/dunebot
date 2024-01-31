package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.Setting;

public interface SettingRepository extends JpaRepository<Setting, Long> {
    Setting findByKeyIgnoreCase(String key);
}
