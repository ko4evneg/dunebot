package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.WrongNamesInputException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.LogId;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.util.ParsedNames;

/**
 * Updates player profile data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshProfileCommandProcessor extends CommandProcessor {
    private static final String SUCCESSFUL_UPDATE_MESSAGE = "Данные профиля обновлены.";

    private final PlayerRepository playerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: REFRESH_PROFILE started", logId());

        playerRepository.findByExternalId(commandMessage.getUserId())
                .ifPresent(player -> {
                    log.debug("{}: player {} found", logId(), player.getId());
                    String allArguments = commandMessage.getAllArguments();
                    if (allArguments.isBlank()) {
                        updateAndSaveTelegramProperties(commandMessage, player);
                    } else {
                        try {
                            updatePlayerNames(player, allArguments);
                            if (player.isGuest()) {
                                player.setGuest(false);
                            }
                        } catch (WrongNamesInputException exception) {
                            throw new AnswerableDuneBotException(exception.getMessage(), commandMessage);
                        } finally {
                            updateAndSaveTelegramProperties(commandMessage, player);
                            MessageDto messageDto = new MessageDto(commandMessage, new ExternalMessage(SUCCESSFUL_UPDATE_MESSAGE), null);
                            messagingService.sendMessageAsync(messageDto);
                        }
                    }
                });

        log.debug("{}: REFRESH_PROFILE ended", logId());
    }

    private void updateAndSaveTelegramProperties(CommandMessage commandMessage, Player player) {
        String firstName = commandMessage.getExternalFirstName();
        String userName = commandMessage.getUserName();
        player.setExternalFirstName(firstName);
        player.setExternalName(userName);
        playerRepository.save(player);
        log.debug("{}: player telegram {} names saved (name: {}, username: {})", LogId.get(), player.getId(), firstName, userName);
    }

    private void updatePlayerNames(Player player, String allArguments) throws WrongNamesInputException {
        ParsedNames parsedNames = new ParsedNames(allArguments);
        player.setFirstName(parsedNames.getFirstName());
        player.setLastName(parsedNames.getLastName());
        player.setSteamName(parsedNames.getSteamName());

        log.debug("{}: player {} names saved. '{}'", LogId.get(), player.getId(), parsedNames);
    }

    @Override
    public Command getCommand() {
        return Command.REFRESH_PROFILE;
    }
}
