package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

/**
 * Deprecated - stub message
 */
@Slf4j
@Service
@Deprecated(since = "0.1.27")
public class NewCommandProcessor extends CommandProcessor {
    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: NEW fallback", logId());
        ExternalMessage message = new ExternalMessage(
                "Команда упразднена! Для создания матчей используйте команды: /new_dune, /new_up4, /new_up6");
        MessageDto messageDto = new MessageDto(commandMessage, message, null);
        messagingService.sendMessageAsync(messageDto);
    }

    @Override
    public Command getCommand() {
        return Command.NEW;
    }
}
