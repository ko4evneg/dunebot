package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.dto.MessageCommand;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
class TelegramUpdateMessageValidator {
    private final PlayerRepository playerRepository;
    private static final String ANONYMOUS_COMMAND_CALL = "Команду могут выполнять только зарегистрированные игроки! Для регистрации выполните команду \"/register *steam_nickname*\"";
    private static final String WRONG_COMMAND = "Неверная команда!";
    private static final String WRONG_REGISTER_COMMAND = "Неверный формат команды! Пример правильной команды: \"/register *steam_nickname*\"";
    private static final String PUBLIC_PROHIBITED_COMMAND = "Команда запрещена в групповых чатах - напишите боту напрямую.";

    private static final Set<Command> publicChatProhibitedCommands = EnumSet.of(Command.REGISTER, Command.SUBMIT);

    public void validate(MessageCommand messageCommand) {
        long telegramChatId = messageCommand.getTelegramChatId();
        Integer replyMessageId = messageCommand.getReplyMessageId();
        Command command = messageCommand.getCommand();
        if (command == null) {
            throw new AnswerableDuneBotException(WRONG_COMMAND, telegramChatId, replyMessageId);
        }
        if (command.getArgumentsCount() > messageCommand.getArgumentsCount()) {
            throw new AnswerableDuneBotException(WRONG_REGISTER_COMMAND, telegramChatId, replyMessageId);
        }
        if (!command.isAnonymous() && !playerRepository.existsByTelegramId(messageCommand.getTelegramUserId())) {
            throw new AnswerableDuneBotException(ANONYMOUS_COMMAND_CALL, telegramChatId, replyMessageId);
        }
        // TODO:  make all commands private?
        if (publicChatProhibitedCommands.contains(command) && ChatType.PRIVATE != messageCommand.getChatType()) {
            throw new AnswerableDuneBotException(PUBLIC_PROHIBITED_COMMAND, telegramChatId, replyMessageId);
        }
    }
}
