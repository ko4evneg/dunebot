package ru.trainithard.dunebot.service.telegram.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class CommonCommandMessageValidator {
    private static final String ANONYMOUS_COMMAND_CALL = "Команду могут выполнять только зарегистрированные игроки! " +
                                                         "Для регистрации выполните команду '/register *steam_name*'";
    private static final String WRONG_COMMAND_TEXT = "Неверная команда!";
    private static final String PUBLIC_PROHIBITED_COMMAND_TEXT = "Команда запрещена в групповых чатах - напишите боту напрямую.";
    private static final String BOT_NOT_CONFIGURED = "Бот не настроен. Разрешены только административные команды.";

    private final PlayerRepository playerRepository;
    private final SettingsService settingsService;

    public void validate(CommandMessage commandMessage) {
        Command command = commandMessage.getCommand();
        if (command == null) {
            throw new AnswerableDuneBotException(WRONG_COMMAND_TEXT, commandMessage);
        }
        if (!command.isAnonymous() && !playerRepository.existsNonGuestByTelegramId(commandMessage.getUserId())) {
            throw new AnswerableDuneBotException(ANONYMOUS_COMMAND_CALL, commandMessage);
        }
        if (command != Command.ADMIN) {
            String stringSetting = settingsService.getStringSetting(SettingKey.CHAT_ID);
            if (stringSetting == null) {
                throw new AnswerableDuneBotException(BOT_NOT_CONFIGURED, commandMessage);
            }
            Integer classicTopicId = settingsService.getIntSetting(SettingKey.TOPIC_ID_CLASSIC);
            if (classicTopicId == null) {
                throw new AnswerableDuneBotException(BOT_NOT_CONFIGURED, commandMessage);
            }
            Integer uprisingTopicId = settingsService.getIntSetting(SettingKey.TOPIC_ID_UPRISING);
            if (uprisingTopicId == null) {
                throw new AnswerableDuneBotException(BOT_NOT_CONFIGURED, commandMessage);
            }
        }
        if (!command.isPublicCommand() && commandMessage.getChatType() != null && commandMessage.getChatType() != ChatType.PRIVATE) {
            throw new AnswerableDuneBotException(PUBLIC_PROHIBITED_COMMAND_TEXT, commandMessage);
        }
    }
}
