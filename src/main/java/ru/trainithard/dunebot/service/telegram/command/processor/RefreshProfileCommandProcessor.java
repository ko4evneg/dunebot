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
import ru.trainithard.dunebot.util.ParsedNames;

@Service
@RequiredArgsConstructor
public class RefreshProfileCommandProcessor extends CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RefreshProfileCommandProcessor.class);
    private static final String SUCCESSFUL_UPDATE_MESSAGE = "Данные профиля обновлены.";

    private final PlayerRepository playerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: refresh started", loggingId);

        playerRepository.findByExternalId(commandMessage.getUserId())
                .ifPresent(player -> {
                    logger.debug("{}: player found, id: {}", loggingId, player.getId());
                    String allArguments = commandMessage.getAllArguments();
                    if (allArguments.isBlank()) {
                        updateAndSaveTelegramProperties(commandMessage, player, loggingId);
                    } else {
                        try {
                            updatePlayerNames(player, allArguments, loggingId);
                        } catch (WrongNamesInputException exception) {
                            throw new AnswerableDuneBotException(exception.getMessage(), commandMessage);
                        } finally {
                            updateAndSaveTelegramProperties(commandMessage, player, loggingId);
                            MessageDto messageDto = new MessageDto(commandMessage, SUCCESSFUL_UPDATE_MESSAGE, null);
                            messagingService.sendMessageAsync(messageDto);
                        }
                    }
                });

        logger.debug("{}: refresh ended", loggingId);
    }

    private void updateAndSaveTelegramProperties(CommandMessage commandMessage, Player player, int loggingId) {
        player.setExternalFirstName(commandMessage.getExternalFirstName());
        player.setExternalName(commandMessage.getUserName());
        playerRepository.save(player);

        logger.debug("{}: player telegram properties set and saved, id: {}", loggingId, player.getId());
    }

    private void updatePlayerNames(Player player, String allArguments, int loggingId) throws WrongNamesInputException {
        ParsedNames parsedNames = new ParsedNames(allArguments);
        player.setFirstName(parsedNames.getFirstName());
        player.setLastName(parsedNames.getLastName());
        player.setSteamName(parsedNames.getSteamName());

        logger.debug("{}: player names set and saved, id: {}", loggingId, player.getId());
    }

    @Override
    public Command getCommand() {
        return Command.REFRESH_PROFILE;
    }
}
