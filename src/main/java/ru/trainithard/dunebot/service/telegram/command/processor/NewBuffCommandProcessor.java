package ru.trainithard.dunebot.service.telegram.command.processor;

import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.telegram.command.Command;

@Service
public class NewBuffCommandProcessor extends NewMatchCommandProcessor {
    @Override
    public Command getCommand() {
        return Command.NEW_BUFF;
    }
}
