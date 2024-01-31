package ru.trainithard.dunebot.service.telegram.command.processor.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.SetCommandsDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminCommandProcessor extends CommandProcessor {
    private static final String HELP_COMMAND_DESCRIPTION = "Как пользоваться ботом";
    private static final String COMMANDS_LIST_DESCRIPTION = "Показать список доступных команд";
    private static final String HELP_COMMAND_TEXT = "/help";
    private static final String COMMANDS_LIST_COMMAND_TEXT = "/commands";
    private static final String INIT_SUBCOMMAND = "init";
    private static final String SET_CHAT_SUBCOMMAND = "set_chat";
    private static final String SET_TOPIC_CLASSIC = "set_topic_dune";
    private static final String SET_TOPIC_UPRISING4 = "set_topic_up4";
    private static final String WRONG_COMMAND_EXCEPTION_MESSAGE = "Неверная команда!";

    private final MessagingService messagingService;
    private final SettingsService settingsService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        String subCommand = commandMessage.getArgument(1).toLowerCase();
        switch (subCommand) {
            case INIT_SUBCOMMAND -> sendSetCommands();
            case SET_CHAT_SUBCOMMAND ->
                    settingsService.saveSetting(SettingsService.CHAT_ID_KEY, Long.toString(commandMessage.getChatId()));
            case SET_TOPIC_CLASSIC ->
                    settingsService.saveSetting(SettingsService.TOPIC_ID_CLASSIC_KEY, commandMessage.getReplyMessageId().toString());
            case SET_TOPIC_UPRISING4 ->
                    settingsService.saveSetting(SettingsService.TOPIC_ID_UPRISING_KEY, commandMessage.getReplyMessageId().toString());
            default -> throw new AnswerableDuneBotException(WRONG_COMMAND_EXCEPTION_MESSAGE, commandMessage);
        }
    }

    private void sendSetCommands() {
        Map<String, String> commands = Map.of(HELP_COMMAND_TEXT, HELP_COMMAND_DESCRIPTION,
                COMMANDS_LIST_COMMAND_TEXT, COMMANDS_LIST_DESCRIPTION);
        messagingService.sendSetCommands(new SetCommandsDto(commands));
    }

    @Override
    public Command getCommand() {
        return Command.ADMIN;
    }
}
