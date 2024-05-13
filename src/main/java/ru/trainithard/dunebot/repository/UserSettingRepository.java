package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.UserSetting;
import ru.trainithard.dunebot.model.UserSettingKey;

import java.util.List;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    UserSetting findByKey(UserSettingKey key);

    List<UserSetting> findAllByPlayerId(long playerId);
}
