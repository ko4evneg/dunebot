package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.MatchCommandProcessor;
import ru.trainithard.dunebot.service.telegram.command.MessageCommand;

@Service
@RequiredArgsConstructor
public class NewCommandProcessor implements CommandProcessor {
    private final MatchCommandProcessor matchCommandProcessor;

    @Override
    public void process(MessageCommand messageCommand) {
        String modTypeString = messageCommand.getArgument(1);
        ModType modType = ModType.getByAlias(modTypeString);
        // TODO:  test
        if (modType == null) {
            throw new AnswerableDuneBotException("Неизвестный тип игры: " + modTypeString, messageCommand.getTelegramChatId(), messageCommand.getReplyMessageId());
        }
        matchCommandProcessor.registerNewMatch(messageCommand.getTelegramUserId(), modType);
    }

    @Override
    public Command getCommand() {
        return Command.NEW;
    }
}
