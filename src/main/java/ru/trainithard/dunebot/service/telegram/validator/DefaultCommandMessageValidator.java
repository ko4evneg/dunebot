package ru.trainithard.dunebot.service.telegram.validator;

import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.CommandType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
public class DefaultCommandMessageValidator implements ValidationStrategy {
    @Override
    public void validate(CommandMessage commandMessage) {
    }

    @Override
    public CommandType getCommandType() {
        return null;
    }
}
