package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.MatchCommandProcessor;
import ru.trainithard.dunebot.service.telegram.command.MessageCommand;

@Service
@RequiredArgsConstructor
public class SubmitCommandProcessor implements CommandProcessor {
    private final MatchCommandProcessor matchCommandProcessor;

    @Override
    public void process(MessageCommand messageCommand) {
        matchCommandProcessor.getSubmitMessage(messageCommand.getTelegramUserId(), messageCommand.getTelegramChatId(), messageCommand.getArgument(1));
    }

    @Override
    public Command getCommand() {
        return Command.SUBMIT;
    }
}
