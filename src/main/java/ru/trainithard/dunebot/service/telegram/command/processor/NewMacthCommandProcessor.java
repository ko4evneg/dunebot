package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.MatchCreationService;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

/**
 * Creates new poll in external messaging system for new match gathering.
 */
@Slf4j
public abstract class NewMacthCommandProcessor extends CommandProcessor {
    @Autowired
    MatchCreationService matchCreationService;

    @Override
    public void process(CommandMessage commandMessage) {
        int logId = logId();
        Command command = commandMessage.getCommand();
        log.debug("{}: {} started", logId, command);

        ModType modType = ModType.getByCommand(commandMessage.getCommand());
        matchCreationService.createMatch(commandMessage.getUserId(), modType);

        log.debug("{}: {} ended", logId, command);
    }
}
