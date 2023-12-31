package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.PlayerService;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class RegisterCommandProcessor implements CommandProcessor {
    private final PlayerService playerService;

    @Override
    public void process(CommandMessage commandMessage) {
        playerService.registerNewPlayer(new PlayerRegistrationDto(commandMessage));
    }

    @Override
    public Command getCommand() {
        return Command.REGISTER;
    }
}
