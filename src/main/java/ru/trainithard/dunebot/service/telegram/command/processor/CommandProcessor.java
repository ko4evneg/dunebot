package ru.trainithard.dunebot.service.telegram.command.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.service.LogId;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

/**
 * Core abstract class for processing <code>CommandMessage</code> representing external messaging system commands.
 */
public abstract class CommandProcessor {
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    MessagingService messagingService;

    public abstract void process(CommandMessage commandMessage);

    public abstract Command getCommand();

    int logId() {
        return LogId.get();
    }
}
