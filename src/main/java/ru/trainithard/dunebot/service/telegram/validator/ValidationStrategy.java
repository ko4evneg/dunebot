package ru.trainithard.dunebot.service.telegram.validator;

import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandType;

public interface ValidationStrategy {
    void validate(CommandMessage commandMessage);

    CommandType getCommandType();
}
