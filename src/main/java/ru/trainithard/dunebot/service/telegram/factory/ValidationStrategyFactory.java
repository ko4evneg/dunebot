package ru.trainithard.dunebot.service.telegram.factory;

import ru.trainithard.dunebot.service.telegram.command.CommandType;
import ru.trainithard.dunebot.service.telegram.validator.ValidationStrategy;

/**
 * Selects suitable validator for external command input.
 */
public interface ValidationStrategyFactory {
    ValidationStrategy getValidator(CommandType commandType);
}
