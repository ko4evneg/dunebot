package ru.trainithard.dunebot.service.telegram.factory;

import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

public interface CommandProcessorFactory {
    CommandProcessor getProcessor(Command command);
}
