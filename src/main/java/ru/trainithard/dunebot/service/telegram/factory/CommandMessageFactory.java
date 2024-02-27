package ru.trainithard.dunebot.service.telegram.factory;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

/**
 * Create application-specific DTO representing command based on external messaging system input.
 */
public interface CommandMessageFactory {
    CommandMessage getInstance(Update update);
}
