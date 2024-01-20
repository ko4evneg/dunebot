package ru.trainithard.dunebot.service.telegram.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommandProcessorFactoryImpl implements CommandProcessorFactory {
    private final List<CommandProcessor> commandProcessors;

    @Override
    public CommandProcessor getProcessor(Command command) {
        return commandProcessors.stream()
                .filter(commandProcessor -> command.equals(commandProcessor.getCommand()))
                .findFirst()
                .orElseThrow(() -> new DuneBotException("Не найден обработчик команды!"));
    }
}
