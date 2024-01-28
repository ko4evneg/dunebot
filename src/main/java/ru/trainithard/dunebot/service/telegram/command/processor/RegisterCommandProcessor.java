package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.WrongNamesInputException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.util.MarkdownEscaper;
import ru.trainithard.dunebot.util.ParsedNames;

@Service
@RequiredArgsConstructor
public class RegisterCommandProcessor extends CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RegisterCommandProcessor.class);
    private static final String NICKNAME_IS_BUSY_MESSAGE_TEMPLATE = "Пользователь со steam ником %s уже существует!";
    private static final String ALREADY_REGISTERED_MESSAGE_TEMPLATE = "Вы уже зарегистрированы под steam ником %s! " +
                                                                      "Для смены ника выполните команду *'/refresh_profile Имя (steam никнейм) Фамилия'*";
    private static final String REGISTRATION_MESSAGE_TEMPLATE = "Вы зарегистрированы как '%s'";

    private final PlayerRepository playerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: register started", loggingId);

        ParsedNames parsedNames = validate(commandMessage);
        logger.debug("{}: validation passed", loggingId);

        Player player = playerRepository.save(new Player(commandMessage, parsedNames));
        String messageText = String.format(REGISTRATION_MESSAGE_TEMPLATE, player.getFriendlyName());
        MessageDto messageDto = new MessageDto(commandMessage.getChatId(), MarkdownEscaper.getEscaped(messageText),
                commandMessage.getReplyMessageId(), null);
        messagingService.sendMessageAsync(messageDto);

        logger.debug("{}: register ended", loggingId);
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
