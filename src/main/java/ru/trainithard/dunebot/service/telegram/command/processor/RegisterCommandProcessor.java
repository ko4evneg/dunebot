package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class RegisterCommandProcessor extends CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RegisterCommandProcessor.class);
    private static final String NICKNAME_IS_BUSY_MESSAGE_TEMPLATE = "Пользователь со steam ником %s уже существует!";
    private static final String ALREADY_REGISTERED_MESSAGE_TEMPLATE = "Вы уже зарегистрированы под steam ником %s! " +
            "Для смены ника выполните команду '/change_steam_name *new_name*'";
    private static final String REGISTRATION_MESSAGE_TEMPLATE = "Вы зарегистрированы под steam-именем ";

    private final PlayerRepository playerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: register started", loggingId);

        validate(commandMessage);
        logger.debug("{}: validation passed", loggingId);

        Player player = playerRepository.save(new Player(commandMessage));
        String messageText = REGISTRATION_MESSAGE_TEMPLATE + player.getSteamName();
        messagingService.sendMessageAsync(
                new MessageDto(commandMessage.getChatId(), messageText, commandMessage.getReplyMessageId(), null));

        logger.debug("{}: register ended", loggingId);
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
