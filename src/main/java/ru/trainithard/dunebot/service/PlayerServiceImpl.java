package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {
    private final PlayerRepository playerRepository;

    @Override
    public void registerNewPlayer(PlayerRegistrationDto playerRegistration) {
        long telegramId = playerRegistration.getTelegramId();
        String steamName = playerRegistration.getSteamName();
        validate(playerRegistration, telegramId, steamName);

        Player player = new Player();
        player.setExternalId(telegramId);
        player.setExternalChatId(playerRegistration.getTelegramChatId());
        player.setFirstName(playerRegistration.getFirstName());
        player.setLastName(playerRegistration.getLastName());
        player.setExternalName(playerRegistration.getUserName());
        player.setSteamName(steamName);
        playerRepository.save(player);
    }

    private void validate(PlayerRegistrationDto playerRegistration, long telegramId, String steamName) {
        playerRepository.findByExternalIdOrSteamName(telegramId, steamName).ifPresent(player -> {
            if (telegramId == player.getExternalId()) {
                throw new AnswerableDuneBotException("Вы уже зарегистрированы под steam ником " + player.getSteamName() + "! Для смены ника выполните команду \"/change_steam_name *new_name*\"", player.getExternalChatId());
            } else if (playerRegistration.getSteamName().equals(steamName)) {
                throw new AnswerableDuneBotException("Пользователь со steam ником " + steamName + " уже существует!", player.getExternalChatId());
            }
        });
    }
}
