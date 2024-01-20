package ru.trainithard.dunebot.service.telegram.validator;

import ru.trainithard.dunebot.model.CommandType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

public interface ValidationStrategy {
    void validate(CommandMessage commandMessage);

    CommandType getCommandType();
}
