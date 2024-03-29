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
                                                                      "Для смены ника выполните команду '/refresh_profile Имя (steam никнейм) Фамилия'";
    private static final String REGISTRATION_MESSAGE_TEMPLATE = "Вы зарегистрированы как '%s'";

    private final PlayerRepository playerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: REGISTER started", logId());

        ParsedNames parsedNames = validate(commandMessage);

        Player player = playerRepository.save(Player.createRegularPlayer(commandMessage, parsedNames));
        log.debug("{}: new player {} saved", logId(), player.getId());
        String messageText = String.format(REGISTRATION_MESSAGE_TEMPLATE, player.getFriendlyName());
        messagingService.sendMessageAsync(
                new MessageDto(commandMessage.getChatId(), new ExternalMessage(messageText), commandMessage.getReplyMessageId(), null));

        log.debug("{}: REGISTER ended", logId());
    }

    private ParsedNames validate(CommandMessage commandMessage) {
        log.debug("{}: names validation", logId());
        try {
            long externalId = commandMessage.getUserId();
            ParsedNames parsedNames = new ParsedNames(commandMessage.getAllArguments());
            String newSteamName = parsedNames.getSteamName();
            playerRepository.findByExternalIdOrSteamName(externalId, newSteamName).ifPresent(player -> {
                long playerExternalId = player.getExternalId();
                String playerSteamName = player.getSteamName();
                log.debug("{}: validation failed, player found: ({}, {})", logId(), playerExternalId, playerSteamName);
                if (externalId == playerExternalId) {
                    throw new AnswerableDuneBotException(String.format(ALREADY_REGISTERED_MESSAGE_TEMPLATE, playerSteamName), player.getExternalChatId());
                } else if (newSteamName.equals(playerSteamName)) {
                    throw new AnswerableDuneBotException(String.format(NICKNAME_IS_BUSY_MESSAGE_TEMPLATE, newSteamName), player.getExternalChatId());
                }
            });
            log.debug("{}: validation successful", logId());
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
