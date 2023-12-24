package ru.trainithard.dunebot.service;

import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;

public interface PlayerService {
    void registerNewPlayer(PlayerRegistrationDto playerRegistration);
}
