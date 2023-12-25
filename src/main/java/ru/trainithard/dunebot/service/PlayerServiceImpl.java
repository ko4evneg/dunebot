package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDubeBotException;
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
        player.setTelegramId(telegramId);
        player.setTelegramChatId(playerRegistration.getTelegramChatId());
        player.setFirstName(playerRegistration.getFirstName());
        player.setLastName(playerRegistration.getLastName());
        player.setUserName(playerRegistration.getUserName());
        player.setSteamName(steamName);
        playerRepository.save(player);
    }

    private void validate(PlayerRegistrationDto playerRegistration, long telegramId, String steamName) {
        playerRepository.findByTelegramIdOrSteamName(telegramId, steamName).ifPresent(player -> {
            if (telegramId == player.getTelegramId()) {
                throw new AnswerableDubeBotException("Вы уже зарегистрированы под steam ником " + player.getSteamName() + "!", player.getTelegramChatId());
            } else if (playerRegistration.getSteamName().equals(steamName)) {
                throw new AnswerableDubeBotException("Пользователь со steam ником " + steamName + " уже существует!", player.getTelegramChatId());
            }
        });
    }
}
