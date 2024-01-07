package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class AcceptSubmitCommandProcessor extends CommandProcessor {
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final MatchRepository matchRepository;

    @Override
    public void process(CommandMessage commandMessage) {

    }

    @Override
    public Command getCommand() {
        return Command.ACCEPT_SUBMIT;
    }
}
