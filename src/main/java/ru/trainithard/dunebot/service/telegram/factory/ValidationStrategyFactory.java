package ru.trainithard.dunebot.service.telegram.factory;

import ru.trainithard.dunebot.model.CommandType;
import ru.trainithard.dunebot.service.telegram.validator.ValidationStrategy;

public interface ValidationStrategyFactory {
    ValidationStrategy getValidator(CommandType commandType);
}
