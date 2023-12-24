package ru.trainithard.dunebot.service.telegram;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.exception.AnswerableDubeBotException;
import ru.trainithard.dunebot.model.Command;

@Service
class TelegramUpdateMessageValidator {
    public void validate(Message message) {
        String text = message.getText();
        if (text.length() < 2) {
            throw new AnswerableDubeBotException("Неверная команда!", message);
        }
        String[] commandWithArguments = text.substring(1).split("\\s");
        String textCommand = commandWithArguments[0];
        Command command = Command.getCommand(textCommand);
        if (command == null) {
            throw new AnswerableDubeBotException("Неверная команда!", message);
        }
        if (command == Command.REGISTER && commandWithArguments.length < 2) {
            throw new AnswerableDubeBotException("Неверный формат команды! Пример правильной команды: \"/register *steam_nickname*\"", message);
        }
    }
}
