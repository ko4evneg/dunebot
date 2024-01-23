package ru.trainithard.dunebot.service.telegram.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandType;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TelegramTextCommandValidator implements ValidationStrategy {
    private static final String ANONYMOUS_COMMAND_CALL = "Команду могут выполнять только зарегистрированные игроки! Для регистрации выполните команду '/register *steam_name*'";
    private static final String WRONG_COMMAND = "Неверная команда!";
    private static final String WRONG_ARGUMENTS_COUNT = "Данная команда должна иметь как минимум один аргумент. Например '/register *steam_name*'";
    private static final String PUBLIC_PROHIBITED_COMMAND = "Команда запрещена в групповых чатах - напишите боту напрямую.";
    private static final Set<Command> publicChatProhibitedCommands = EnumSet.of(Command.REGISTER, Command.SUBMIT);

    private final PlayerRepository playerRepository;

    public void validate(CommandMessage commandMessage) {
        Command command = commandMessage.getCommand();
        if (command == null) {
            throw new AnswerableDuneBotException(WRONG_COMMAND, commandMessage);
        }
        if (!command.isAnonymous() && !playerRepository.existsByTelegramId(commandMessage.getUserId())) {
            throw new AnswerableDuneBotException(ANONYMOUS_COMMAND_CALL, commandMessage);
        }
        if (publicChatProhibitedCommands.contains(command) && ChatType.PRIVATE != commandMessage.getChatType()) {
            throw new AnswerableDuneBotException(PUBLIC_PROHIBITED_COMMAND, commandMessage);
        }
        if (command.getMinimalArgumentsCount() > commandMessage.getArgumentsCount()) {
            throw new AnswerableDuneBotException(WRONG_ARGUMENTS_COUNT, commandMessage);
        }
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.TEXT;
    }
}
