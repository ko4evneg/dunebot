package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class StartCommandProcessor extends CommandProcessor {
    private final HelpCommandProcessor helpCommandProcessor;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        helpCommandProcessor.process(commandMessage, loggingId);
    }

    @Override
    public Command getCommand() {
        return Command.START;
    }

}
