package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.MatchCommandProcessor;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class CancelCommandProcessor implements CommandProcessor {
    private final MatchCommandProcessor matchCommandProcessor;

    @Override
    public void process(CommandMessage commandMessage) {
        matchCommandProcessor.cancelMatch(commandMessage.getUserId());
    }

    @Override
    public Command getCommand() {
        return Command.CANCEL;
    }
}
