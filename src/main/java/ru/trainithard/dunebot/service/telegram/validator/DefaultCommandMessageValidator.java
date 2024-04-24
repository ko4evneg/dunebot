package ru.trainithard.dunebot.service.telegram.validator;

import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandType;

@Service
public class DefaultCommandMessageValidator implements ValidationStrategy {
    @Override
    public void validate(CommandMessage commandMessage) {
        //This method used when no other validators present. Such a case require successful validation now.
    }

    @Override
    public CommandType getCommandType() {
        return null;
    }
}
