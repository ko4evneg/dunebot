package ru.trainithard.dunebot.service.telegram.command.processor;

import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

public interface CommandProcessor {
    void process(CommandMessage commandMessage);

    Command getCommand();
}
