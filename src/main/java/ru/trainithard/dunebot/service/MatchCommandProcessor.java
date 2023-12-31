package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;

@Service
@RequiredArgsConstructor
public class MatchCommandProcessor {
    private final PlayerService playerService;

    public void registerNewPlayer(PlayerRegistrationDto playerRegistration) {
        playerService.registerNewPlayer(playerRegistration);
    }
}
