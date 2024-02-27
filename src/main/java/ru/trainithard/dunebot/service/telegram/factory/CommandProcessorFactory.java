package ru.trainithard.dunebot.service.telegram.factory;

import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

/**
 * Selects <code>CommandProcessor</code> responsible for command processing.
 */
public interface CommandProcessorFactory {
    CommandProcessor getProcessor(Command command);
}
