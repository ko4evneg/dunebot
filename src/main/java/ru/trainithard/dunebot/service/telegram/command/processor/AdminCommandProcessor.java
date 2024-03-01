package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

/**
 * Process admin commands for bot configuration and management.
 */
@Slf4j
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
    private static final String SUCCESSFUL_COMMAND_TEXT = "Команда успешно выполнена.";

    private final MessagingService messagingService;
    private final SettingsService settingsService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        log.debug("{}: admin command started with args: '{}'", loggingId, commandMessage.getAllArguments());

        String subCommand = commandMessage.getArgument(1).toLowerCase();

        MessageDto messageDto = new MessageDto(commandMessage, new ExternalMessage(SUCCESSFUL_COMMAND_TEXT), null);

        switch (subCommand) {
            case INIT_SUBCOMMAND -> sendSetCommands();
            case SET_CHAT_SUBCOMMAND ->
                    settingsService.saveSetting(SettingKey.CHAT_ID, Long.toString(commandMessage.getChatId()));
            case SET_TOPIC_CLASSIC ->
                    settingsService.saveSetting(SettingKey.TOPIC_ID_CLASSIC, commandMessage.getReplyMessageId().toString());
            case SET_TOPIC_UPRISING4 ->
                    settingsService.saveSetting(SettingKey.TOPIC_ID_UPRISING, commandMessage.getReplyMessageId().toString());
            default -> {
                log.debug("{}: wronad admin command subcommand {}", loggingId, subCommand);
                messageDto.setText(WRONG_COMMAND_EXCEPTION_MESSAGE);
            }
        }

        messagingService.sendMessageAsync(messageDto);
        log.debug("{}: admin command sucsessfuly finished execution", loggingId, commandMessage, subCommand);
    }

    private void sendSetCommands() {
        // TODO:  replace with valid list
//        Map<String, String> commands = Map.of(HELP_COMMAND_TEXT, HELP_COMMAND_DESCRIPTION,
//                COMMANDS_LIST_COMMAND_TEXT, COMMANDS_LIST_DESCRIPTION);
//        messagingService.sendSetCommands(new SetCommandsDto(commands));
    }

    @Override
    public Command getCommand() {
        return Command.ADMIN;
    }
}
