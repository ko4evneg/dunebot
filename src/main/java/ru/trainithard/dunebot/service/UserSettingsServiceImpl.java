package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.UserSetting;
import ru.trainithard.dunebot.model.UserSettingKey;
import ru.trainithard.dunebot.repository.UserSettingRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {
    private final UserSettingRepository userSettingRepository;

    @Override
    public Optional<UserSetting> getSetting(long playerId, UserSettingKey key) {
        return userSettingRepository.findByPlayerIdAndKey(playerId, key);
    }

    @Override
    public List<UserSetting> getAllSettings(long playerId) {
        return userSettingRepository.findAllByPlayerId(playerId);
    }

    @Override
    public void saveSetting(long playerId, UserSettingKey key, String value) {
        Optional<UserSetting> existingSettingOptional = userSettingRepository.findByPlayerIdAndKey(playerId, key);
        if (existingSettingOptional.isPresent()) {
            UserSetting existingSetting = existingSettingOptional.get();
            existingSetting.setValue(value);
            userSettingRepository.save(existingSetting);
        } else {
            Player player = new Player();
            player.setId(playerId);
            userSettingRepository.save(new UserSetting(player, key, value));
        }
    }
}
