package ru.trainithard.dunebot.service.telegram.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.CommandType;
import ru.trainithard.dunebot.service.telegram.validator.DefaultCommandMessageValidator;
import ru.trainithard.dunebot.service.telegram.validator.ValidationStrategy;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationStrategyFactoryImpl implements ValidationStrategyFactory {
    private final List<ValidationStrategy> validationStrategies;
    private final DefaultCommandMessageValidator defaultCommandMessageValidator;

    @Override
    public ValidationStrategy getValidator(CommandType commandType) {

        return validationStrategies.stream()
                .filter(validationStrategy -> commandType.equals(validationStrategy.getCommandType()))
                .findFirst()
                .orElse(defaultCommandMessageValidator);
    }
}
