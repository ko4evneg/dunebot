package ru.trainithard.dunebot.service.telegram.factory;

import ru.trainithard.dunebot.service.telegram.command.CommandType;
import ru.trainithard.dunebot.service.telegram.validator.ValidationStrategy;

public interface ValidationStrategyFactory {
    ValidationStrategy getValidator(CommandType commandType);
}
