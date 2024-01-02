package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class RegisterCommandProcessor extends CommandProcessor {
    private final PlayerRepository playerRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        validate(commandMessage);
        playerRepository.save(new Player(commandMessage));
    }

    private void validate(CommandMessage commandMessage) {
        long externalId = commandMessage.getUserId();
        String steamName = commandMessage.getAllArguments();
        playerRepository.findByExternalIdOrSteamName(externalId, steamName).ifPresent(player -> {
            if (externalId == player.getExternalId()) {
                throw new AnswerableDuneBotException("Вы уже зарегистрированы под steam ником " + player.getSteamName() + "! Для смены ника выполните команду \"/change_steam_name *new_name*\"", player.getExternalChatId());
            } else if (steamName.equals(player.getSteamName())) {
                throw new AnswerableDuneBotException("Пользователь со steam ником " + steamName + " уже существует!", player.getExternalChatId());
            }
        });
    }

    @Override
    public Command getCommand() {
        return Command.REGISTER;
    }
}
