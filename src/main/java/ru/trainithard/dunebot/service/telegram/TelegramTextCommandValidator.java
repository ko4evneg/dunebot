package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
class TelegramTextCommandValidator {
    private final PlayerRepository playerRepository;
    private final Map<Command, CommandProcessor> commandProcessors;

    private static final String ANONYMOUS_COMMAND_CALL = "Команду могут выполнять только зарегистрированные игроки! Для регистрации выполните команду '/register *steam_name*'";
    private static final String WRONG_COMMAND = "Неверная команда!";
    private static final String WRONG_ARGUMENTS_COUNT = "Данная команда должна иметь как минимум один аргумент. Например '/register *steam_name*'";
    private static final String PUBLIC_PROHIBITED_COMMAND = "Команда запрещена в групповых чатах - напишите боту напрямую.";

    private static final Set<Command> publicChatProhibitedCommands = EnumSet.of(Command.REGISTER, Command.SUBMIT);

    public void validate(CommandMessage commandMessage) {
        long telegramChatId = commandMessage.getChatId();
        Integer replyMessageId = commandMessage.getReplyMessageId();
        Command command = commandMessage.getCommand();
        if (command == null || !commandProcessors.containsKey(command)) {
            throw new AnswerableDuneBotException(WRONG_COMMAND, telegramChatId, replyMessageId);
        }
        if (!command.isAnonymous() && !playerRepository.existsByTelegramId(commandMessage.getUserId())) {
            throw new AnswerableDuneBotException(ANONYMOUS_COMMAND_CALL, telegramChatId, replyMessageId);
        }
        // TODO:  make all commands private?
        if (publicChatProhibitedCommands.contains(command) && ChatType.PRIVATE != commandMessage.getChatType()) {
            throw new AnswerableDuneBotException(PUBLIC_PROHIBITED_COMMAND, telegramChatId, replyMessageId);
        }
        if (command.getMinimalArgumentsCount() > commandMessage.getArgumentsCount()) {
            throw new AnswerableDuneBotException(WRONG_ARGUMENTS_COUNT, telegramChatId, replyMessageId);
        }
    }
}
