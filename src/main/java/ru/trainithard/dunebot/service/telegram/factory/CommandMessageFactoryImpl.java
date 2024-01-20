package ru.trainithard.dunebot.service.telegram.factory;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
public class CommandMessageFactoryImpl implements CommandMessageFactory {
    @Override
    public CommandMessage getInstance(Update update) {
        Message message = update.getMessage();
        if (hasSlashPrefixedText(message)) {
            return new CommandMessage(message);
        } else if (hasPollAnswerOption(update)) {
            return new CommandMessage(update.getPollAnswer());
        } else if (hasNotBlackCallbackQuery(update)) {
            return new CommandMessage(update.getCallbackQuery());
        }
        return null;
    }

    private boolean hasSlashPrefixedText(Message message) {
        return message != null && message.getText() != null && message.getText().startsWith("/");
    }

    private boolean hasPollAnswerOption(Update update) {
        return update.hasPollAnswer() && !update.getPollAnswer().getOptionIds().isEmpty();
    }

    private boolean hasNotBlackCallbackQuery(Update update) {
        return update.hasCallbackQuery() && update.getCallbackQuery().getData() != null && !update.getCallbackQuery().getData().isBlank();
    }
}
