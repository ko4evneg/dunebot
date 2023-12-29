package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.telegram.command.MessageCommand;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
    private final TelegramBot telegramBot;
    private final TelegramMessageCommandValidator telegramMessageCommandValidator;
    private final Map<Command, CommandProcessor> commandProcessors;

    @Scheduled(fixedDelay = 500)
    public void process() {
        Update update = telegramBot.poll();
        while (update != null) {
            try {
                Message message = update.getMessage();
                // TODO: validate only valid messages goes further [!!!] commands should contain all complete actions with valid arguments
                if (message != null && message.getText() != null && message.getText().startsWith("/")) {
                    MessageCommand messageCommand = new MessageCommand(message);
                    telegramMessageCommandValidator.validate(messageCommand);
                    commandProcessors.get(messageCommand.getCommand()).process(messageCommand);
                }
            } catch (AnswerableDuneBotException answerableException) {
                sendUserNotificationMessage(answerableException);
            } catch (Exception e) {
                // TODO:
                System.out.println(e.getMessage());
            } finally {
                update = telegramBot.poll();
            }
        }
    }

    private void sendUserNotificationMessage(AnswerableDuneBotException answerableException) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(answerableException.getTelegramChatId());
            sendMessage.setText(answerableException.getMessage());
            if (answerableException.getTelegramTopicId() != null) {
                sendMessage.setReplyToMessageId(answerableException.getTelegramTopicId());
            }
            telegramBot.executeAsync(sendMessage);
        } catch (Exception e) {
            // TODO:
            throw new RuntimeException(e);
        }
    }
}
