package ru.trainithard.dunebot.service.telegram;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.model.Command;

@Service
class TelegramUpdateValidator {
    public boolean isValidCommand(Message message) {
        String text = message.getText();
        return text.startsWith("/") && text.length() > 1 && Command.getCommand(text.substring(1)) != null;
    }
}
