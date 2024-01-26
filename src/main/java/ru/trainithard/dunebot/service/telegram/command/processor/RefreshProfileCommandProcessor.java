package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class RefreshProfileCommandProcessor extends CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RefreshProfileCommandProcessor.class);

    private final PlayerRepository playerRepository;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: refresh started", loggingId);

        playerRepository.findByExternalId(commandMessage.getUserId())
                .ifPresent(player -> {
                    logger.debug("{}: player found, id: {}", loggingId, player.getId());

                    String allArguments = commandMessage.getAllArguments();
                    if (allArguments != null) {
                        player.setSteamName(allArguments);
                    }
                    player.setFirstName(commandMessage.getFirstName());
                    player.setLastName(commandMessage.getLastName());
                    player.setExternalName(commandMessage.getUserName());
                    playerRepository.save(player);
                });

        logger.debug("{}: refresh ended", loggingId);
    }

    @Override
    public Command getCommand() {
        return Command.REFRESH_PROFILE;
    }
}
