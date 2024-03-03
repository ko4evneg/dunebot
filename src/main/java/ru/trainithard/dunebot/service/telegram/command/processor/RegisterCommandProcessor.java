package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.WrongNamesInputException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.util.ParsedNames;

/**
 * Registers new player.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterCommandProcessor extends CommandProcessor {
    private static final String NICKNAME_IS_BUSY_MESSAGE_TEMPLATE = "Пользователь со steam ником %s уже существует!";
    private static final String ALREADY_REGISTERED_MESSAGE_TEMPLATE = "Вы уже зарегистрированы под steam ником %s! " +
                                                                      "Для смены ника выполните команду *'/refresh_profile Имя (steam никнейм) Фамилия'*";
    private static final String REGISTRATION_MESSAGE_TEMPLATE = "Вы зарегистрированы как '%s'";

    private final PlayerRepository playerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        log.debug("{}: register started", loggingId);

        ParsedNames parsedNames = validate(commandMessage);
        log.debug("{}: validation passed", loggingId);

        Player player = playerRepository.save(Player.createRegularPlayer(commandMessage, parsedNames));
        String messageText = String.format(REGISTRATION_MESSAGE_TEMPLATE, player.getFriendlyName());
        messagingService.sendMessageAsync(
                new MessageDto(commandMessage.getChatId(), new ExternalMessage(messageText), commandMessage.getReplyMessageId(), null));

        log.debug("{}: register ended", loggingId);
    }

    private ParsedNames validate(CommandMessage commandMessage) {
        try {
            long externalId = commandMessage.getUserId();
            ParsedNames parsedNames = new ParsedNames(commandMessage.getAllArguments());
            String steamName = parsedNames.getSteamName();
            playerRepository.findByExternalIdOrSteamName(externalId, steamName).ifPresent(player -> {
                if (externalId == player.getExternalId()) {
                    throw new AnswerableDuneBotException(String.format(ALREADY_REGISTERED_MESSAGE_TEMPLATE, player.getSteamName()), player.getExternalChatId());
                } else if (steamName.equals(player.getSteamName())) {
                    throw new AnswerableDuneBotException(String.format(NICKNAME_IS_BUSY_MESSAGE_TEMPLATE, steamName), player.getExternalChatId());
                }
            });
            return parsedNames;
        } catch (WrongNamesInputException exception) {
            throw new AnswerableDuneBotException(exception.getMessage(), commandMessage);
        }
    }

    @Override
    public Command getCommand() {
        return Command.REGISTER;
    }
}
