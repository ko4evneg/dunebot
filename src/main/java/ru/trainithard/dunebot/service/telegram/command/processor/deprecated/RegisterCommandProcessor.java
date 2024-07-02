package ru.trainithard.dunebot.service.telegram.command.processor.deprecated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

/**
 * Registers new player.
 */
@Slf4j
@Service
@Deprecated(since = "0.1.20", forRemoval = true)
@RequiredArgsConstructor
public class RegisterCommandProcessor extends CommandProcessor {
    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: REGISTER fallback", logId());
        ExternalMessage message = new ExternalMessage(
                "Команда упразднена! Используйте единую команду для регистрации или обновления данных: \n/profile Имя (ник-steam) Фамилия");
        MessageDto messageDto = new MessageDto(commandMessage, message, null);
        messagingService.sendMessageAsync(messageDto);
    }

    @Override
    public Command getCommand() {
        return Command.REGISTER;
    }
}
