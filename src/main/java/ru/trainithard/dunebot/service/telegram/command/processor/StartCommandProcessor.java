package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

/**
 * Redirect /start command to /help.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StartCommandProcessor extends CommandProcessor {
    private final HelpCommandProcessor helpCommandProcessor;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        log.debug("{}: START: redirecting to help command", logId());
        helpCommandProcessor.process(commandMessage, logId());
    }

    @Override
    public Command getCommand() {
        return Command.START;
    }

}
