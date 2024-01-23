package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class RegisterCommandProcessor extends CommandProcessor {
    private static final String NICKNAME_IS_BUSY_MESSAGE_TEMPLATE = "Пользователь со steam ником %s уже существует!";
    private static final String ALREADY_REGISTERED_MESSAGE_TEMPLATE = "Вы уже зарегистрированы под steam ником %s! " +
            "Для смены ника выполните команду '/change_steam_name *new_name*'";

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
                throw new AnswerableDuneBotException(String.format(ALREADY_REGISTERED_MESSAGE_TEMPLATE, player.getSteamName()), player.getExternalChatId());
            } else if (steamName.equals(player.getSteamName())) {
                throw new AnswerableDuneBotException(String.format(NICKNAME_IS_BUSY_MESSAGE_TEMPLATE, steamName), player.getExternalChatId());
            }
        });
    }

    @Override
    public Command getCommand() {
        return Command.REGISTER;
    }
}
