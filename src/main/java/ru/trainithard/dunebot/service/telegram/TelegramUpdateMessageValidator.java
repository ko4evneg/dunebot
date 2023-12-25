package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.exception.AnswerableDubeBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.repository.PlayerRepository;

@Service
@RequiredArgsConstructor
class TelegramUpdateMessageValidator {
    public static final String ANONYMOUS_COMMAND_CALL = "Команду могут выполнять только зарегистрированные игроки! Для регистрации выполните команду \"/register *steam_nickname*\"";
    private final PlayerRepository playerRepository;
    public static final String WRONG_COMMAND = "Неверная команда!";
    public static final String WRONG_REGISTER_COMMAND = "Неверный формат команды! Пример правильной команды: \"/register *steam_nickname*\"";

    public void validate(Message message) {
        String text = message.getText();
        Long telegramChatId = message.getChatId();
        if (text.length() < 2) {
            throw new AnswerableDubeBotException(WRONG_COMMAND, telegramChatId);
        }
        String[] commandWithArguments = text.substring(1).split("\\s");
        String textCommand = commandWithArguments[0];
        Command command = Command.getCommand(textCommand);
        if (command == null) {
            throw new AnswerableDubeBotException(WRONG_COMMAND, telegramChatId);
        }
        if (command == Command.REGISTER && commandWithArguments.length < 2) {
            throw new AnswerableDubeBotException(WRONG_REGISTER_COMMAND, telegramChatId);
        }
        if (!command.isAnonymous() && !playerRepository.existsByTelegramId(message.getFrom().getId())) {
            throw new AnswerableDubeBotException(ANONYMOUS_COMMAND_CALL, telegramChatId);
        }
    }
}
