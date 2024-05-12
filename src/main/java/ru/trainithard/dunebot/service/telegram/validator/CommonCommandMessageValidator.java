package ru.trainithard.dunebot.service.telegram.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommonCommandMessageValidator {
    private static final String ANONYMOUS_COMMAND_CALL = "Команду могут выполнять только зарегистрированные игроки! " +
                                                         "Для регистрации выполните команду \n/profile Имя (ник_steam) Фамилия";
    private static final String WRONG_COMMAND_TEXT = "Неверная команда!";
    private static final String PUBLIC_PROHIBITED_COMMAND_TEXT = "Команда запрещена в групповых чатах - напишите боту напрямую.";
    private static final String BOT_NOT_CONFIGURED = "Бот не настроен. Разрешены только административные команды.";
    private static final int TOPIC_QUANTITY = 2;

    private final PlayerRepository playerRepository;
    private final SettingsService settingsService;

    /**
     * Perform validation of the command messages and checks if command processing is required
     *
     * @param commandMessage message to validate
     * @return <code>false</code> if command processing is not required, <code>true</code> otherwise
     * @throws AnswerableDuneBotException when validation fails
     */
    public boolean validate(CommandMessage commandMessage) {
        Command command = commandMessage.getCommand();

        List<Integer> topicIds = getTopicIds();
        if (shouldSkipCommandProcessing(commandMessage, command, topicIds)) {
            return false;
        }
        validateCorrectCommand(commandMessage, command);
        validateAnonymousCallForNonAnonymousCommand(commandMessage, command);
        validateBotIsConfiguredForNonAdminCommands(commandMessage, command, topicIds);
        validatePrivateCommandExecutedInPublicChat(commandMessage, command);
        return true;
    }

    private boolean shouldSkipCommandProcessing(CommandMessage commandMessage, Command command, Collection<Integer> topicIds) {
        boolean isPublicUnknownTopic = commandMessage.getChatType() != ChatType.PRIVATE && !topicIds.contains(commandMessage.getTopicId());
        if (command == null && isPublicUnknownTopic) {
            return true;
        } else if (command == null) {
            return false;
        }
        CommandType commandType = command.getCommandType();
        return (commandType == CommandType.TEXT && command != Command.ADMIN
                || commandType == CommandType.FILE_UPLOAD)
               && isPublicUnknownTopic;
    }

    private void validateCorrectCommand(CommandMessage commandMessage, Command command) {
        if (command == null) {
            throw new AnswerableDuneBotException(WRONG_COMMAND_TEXT, commandMessage);
        }
    }

    private void validateAnonymousCallForNonAnonymousCommand(CommandMessage commandMessage, Command command) {
        if (!command.isAnonymous() && !playerRepository.existsNonGuestByTelegramId(commandMessage.getUserId())) {
            throw new AnswerableDuneBotException(ANONYMOUS_COMMAND_CALL, commandMessage);
        }
    }

    private void validateBotIsConfiguredForNonAdminCommands(CommandMessage commandMessage, Command command, Collection<Integer> topicIds) {
        if (command != Command.ADMIN) {
            String stringSetting = settingsService.getStringSetting(AppSettingKey.CHAT_ID);
            if (stringSetting == null) {
                throw new AnswerableDuneBotException(BOT_NOT_CONFIGURED, commandMessage);
            }
            if (topicIds.size() != TOPIC_QUANTITY) {
                throw new AnswerableDuneBotException(BOT_NOT_CONFIGURED, commandMessage);
            }
        }
    }

    private void validatePrivateCommandExecutedInPublicChat(CommandMessage commandMessage, Command command) {
        if (!command.isPublicCommand() && commandMessage.getChatType() != null && commandMessage.getChatType() != ChatType.PRIVATE) {
            throw new AnswerableDuneBotException(PUBLIC_PROHIBITED_COMMAND_TEXT, commandMessage);
        }
    }

    private List<Integer> getTopicIds() {
        List<Integer> topicIds = new ArrayList<>();
        topicIds.add(settingsService.getIntSetting(AppSettingKey.TOPIC_ID_CLASSIC));
        topicIds.add(settingsService.getIntSetting(AppSettingKey.TOPIC_ID_UPRISING));
        topicIds.removeIf(Objects::isNull);
        return topicIds;
    }
}
