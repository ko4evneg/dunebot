package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.MatchCommandProcessor;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
@RequiredArgsConstructor
public class NewCommandProcessor implements CommandProcessor {
    private final MatchCommandProcessor matchCommandProcessor;

    @Override
    public void process(CommandMessage commandMessage) {
        String modTypeString = commandMessage.getArgument(1);
        ModType modType = ModType.getByAlias(modTypeString);
        // TODO:  test
        if (modType == null) {
            throw new AnswerableDuneBotException("Неизвестный тип игры: " + modTypeString, commandMessage.getChatId(), commandMessage.getReplyMessageId());
        }
        matchCommandProcessor.registerNewMatch(commandMessage.getUserId(), modType);
    }

    @Override
    public Command getCommand() {
        return Command.NEW;
    }
}
