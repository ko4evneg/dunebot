package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.repository.PlayerRepository;

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

    public void validate(Message message) {
        String text = message.getText();
        Long telegramChatId = message.getChatId();
        if (text.length() < 2) {
            throw new AnswerableDuneBotException(WRONG_COMMAND, telegramChatId);
        }
        String[] commandWithArguments = text.substring(1).split("\\s");
        String textCommand = commandWithArguments[0];
        Command command = Command.getCommand(textCommand);
        if (command == null) {
            throw new AnswerableDuneBotException(WRONG_COMMAND, telegramChatId);
        }
        if (command.getWordsCount() > commandWithArguments.length) {
            throw new AnswerableDuneBotException(WRONG_REGISTER_COMMAND, telegramChatId);
        }
        if (!command.isAnonymous() && !playerRepository.existsByTelegramId(message.getFrom().getId())) {
            throw new AnswerableDuneBotException(ANONYMOUS_COMMAND_CALL, telegramChatId);
        }
        if (publicChatProhibitedCommands.contains(command) && !ChatType.PRIVATE.getValue().equals(message.getChat().getType())) {
            throw new AnswerableDuneBotException(PUBLIC_PROHIBITED_COMMAND, telegramChatId);
        }
    }
}
