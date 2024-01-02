package ru.trainithard.dunebot.service.telegram.command.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

public abstract class CommandProcessor {
    @Autowired
    TransactionTemplate transactionTemplate;

    public abstract void process(CommandMessage commandMessage);

    public abstract Command getCommand();
}
