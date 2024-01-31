package ru.trainithard.dunebot.service.telegram.command.processor.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    // TODO:
    private static final String SET_TOPIC_DUNE = "dune_topic";
    // TODO:
    private static final String SET_TOPIC_UPRISING4 = "uprising_topic";

    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        if (INIT_SUBCOMMAND.equalsIgnoreCase(commandMessage.getArgument(1))) {
            Map<String, String> commands = Map.of(HELP_COMMAND_TEXT, HELP_COMMAND_DESCRIPTION,
                    COMMANDS_LIST_COMMAND_TEXT, COMMANDS_LIST_DESCRIPTION);
            messagingService.sendSetCommands(new SetCommandsDto(commands));
        }
    }

    @Override
    public Command getCommand() {
        return Command.ADMIN;
    }
}
