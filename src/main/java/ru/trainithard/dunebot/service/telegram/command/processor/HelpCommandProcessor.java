package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

/**
 * Shows bot how to instructions to requester.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HelpCommandProcessor extends CommandProcessor {
    private final ExternalMessageFactory messageFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: HELP started", logId());
        ExternalMessage helpMessage = messageFactory.getHelpMessage();
        messagingService.sendMessageAsync(new MessageDto(commandMessage, helpMessage, null));
        log.debug("{}: HELP ended", logId());
    }

    @Override
    public Command getCommand() {
        return Command.HELP;
    }
}
