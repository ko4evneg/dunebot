package ru.trainithard.dunebot.service.telegram.command.processor;

import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.command.MessageCommand;

public interface CommandProcessor {
    void process(MessageCommand messageCommand);

    Command getCommand();
}
