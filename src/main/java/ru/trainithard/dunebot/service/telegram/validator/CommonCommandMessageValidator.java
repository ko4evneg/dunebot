package ru.trainithard.dunebot.service.telegram.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class CommonCommandMessageValidator {
    private static final String ANONYMOUS_COMMAND_CALL = "Команду могут выполнять только зарегистрированные игроки! " +
                                                         "Для регистрации выполните команду '/register *steam_name*'";
    private static final String WRONG_COMMAND = "Неверная команда!";
    private static final String PUBLIC_PROHIBITED_COMMAND = "Команда запрещена в групповых чатах - напишите боту напрямую.";

    private final PlayerRepository playerRepository;

    public void validate(CommandMessage commandMessage) {
        Command command = commandMessage.getCommand();
        if (command == null) {
            throw new AnswerableDuneBotException(WRONG_COMMAND, commandMessage);
        }
        if (!command.isAnonymous() && !playerRepository.existsNonGuestByTelegramId(commandMessage.getUserId())) {
            throw new AnswerableDuneBotException(ANONYMOUS_COMMAND_CALL, commandMessage);
        }
        if (!command.isPublicCommand() && commandMessage.getChatType() != null && commandMessage.getChatType() != ChatType.PRIVATE) {
            throw new AnswerableDuneBotException(PUBLIC_PROHIBITED_COMMAND, commandMessage);
        }
    }
}
