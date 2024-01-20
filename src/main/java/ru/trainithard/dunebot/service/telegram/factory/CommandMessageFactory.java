package ru.trainithard.dunebot.service.telegram.factory;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

public interface CommandMessageFactory {
    CommandMessage getInstance(Update update);
}
