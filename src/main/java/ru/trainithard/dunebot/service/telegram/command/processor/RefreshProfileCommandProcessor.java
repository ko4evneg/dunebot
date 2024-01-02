package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class RefreshProfileCommandProcessor extends CommandProcessor {
    private final PlayerRepository playerRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        playerRepository.findByExternalId(commandMessage.getUserId())
                .ifPresent(player -> {
                    String allArguments = commandMessage.getAllArguments();
                    if (allArguments != null) {
                        player.setSteamName(allArguments);
                    }
                    player.setFirstName(commandMessage.getFirstName());
                    player.setLastName(commandMessage.getLastName());
                    player.setExternalName(commandMessage.getUserName());
                    playerRepository.save(player);
                });
    }

    @Override
    public Command getCommand() {
        return Command.REFRESH_PROFILE;
    }
}
