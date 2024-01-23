package ru.trainithard.dunebot.service.telegram.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandType;

@Service
@RequiredArgsConstructor
public class TelegramTextCommandValidator implements ValidationStrategy {
    private static final String WRONG_ARGUMENTS_COUNT = "Данная команда должна иметь как минимум один аргумент. Например '/register *steam_name*'";

    public void validate(CommandMessage commandMessage) {
        int minimalArgumentsCount = commandMessage.getCommand().getMinimalArgumentsCount();
        if (minimalArgumentsCount > commandMessage.getArgumentsCount()) {
            throw new AnswerableDuneBotException(WRONG_ARGUMENTS_COUNT, commandMessage);
        }
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.TEXT;
    }
}
