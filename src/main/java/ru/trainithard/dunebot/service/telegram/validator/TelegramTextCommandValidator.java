package ru.trainithard.dunebot.service.telegram.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandType;

@Service
@RequiredArgsConstructor
public class TelegramTextCommandValidator implements ValidationStrategy {
    private static final String WRONG_ARGUMENTS_COUNT_TEMPLATE = "Данная команда должна иметь %d параметр(а).";
    private static final String NOT_AUTHORIZED_USER = "Команда требует прав администратора.";

    private final AppSettingsService appSettingsService;

    public void validate(CommandMessage commandMessage) {
        int minimalArgumentsCount = commandMessage.getCommand().getMinimalArgumentsCount();
        if (minimalArgumentsCount > commandMessage.getArgumentsCount()) {
            throw new AnswerableDuneBotException(String.format(WRONG_ARGUMENTS_COUNT_TEMPLATE, minimalArgumentsCount), commandMessage);
        }
        long adminId = appSettingsService.getLongSetting(AppSettingKey.ADMIN_USER_ID);
        if (commandMessage.getCommand() == Command.ADMIN && commandMessage.getUserId() != adminId) {
            throw new AnswerableDuneBotException(NOT_AUTHORIZED_USER, commandMessage);
        }
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.TEXT;
    }
}
