package ru.trainithard.dunebot.service.telegram.factory;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Service
public class CommandMessageFactoryImpl implements CommandMessageFactory {
    @Override
    public CommandMessage getInstance(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (hasSlashPrefixedText(message) || hasAttachedPhoto(message)) {
                return CommandMessage.getMessageInstance(message);
            }
        } else if (hasPollAnswerOption(update)) {
            return CommandMessage.getPollAnswerInstance(update.getPollAnswer());
        } else if (hasNotBlankCallbackQuery(update)) {
            return CommandMessage.getCallbackInstance(update.getCallbackQuery());
        }
        return null;
    }

    private boolean hasSlashPrefixedText(Message message) {
        return message.getText() != null && message.getText().startsWith("/");
    }

    private boolean hasPollAnswerOption(Update update) {
        return update.hasPollAnswer();
    }

    private boolean hasNotBlankCallbackQuery(Update update) {
        return update.hasCallbackQuery() && update.getCallbackQuery().getData() != null && !update.getCallbackQuery().getData().isBlank();
    }

    private boolean hasAttachedPhoto(Message message) {
        return message.hasDocument() || message.hasPhoto();
    }
}
