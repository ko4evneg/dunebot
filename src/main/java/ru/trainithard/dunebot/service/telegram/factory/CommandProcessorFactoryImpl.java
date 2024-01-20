package ru.trainithard.dunebot.service.telegram.factory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommandProcessorFactoryImpl implements CommandProcessorFactory {
    private final Map<Command, CommandProcessor> commandProcessors;

    @Override
    public CommandProcessor getProcessor(Command command) {
        return commandProcessors.get(command);
    }
}
