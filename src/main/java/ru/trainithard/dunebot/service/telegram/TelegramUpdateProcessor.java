package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
    private final TelegramBot telegramBot;
    private final MessagingService messagingService;
    private final TelegramMessageCommandValidator telegramMessageCommandValidator;
    private final Map<Command, CommandProcessor> commandProcessors;

    public void process() {
        Update update = telegramBot.poll();
        while (update != null) {
            try {
                Message message = update.getMessage();
                // TODO: validate only valid messages goes further [!!!] commands should contain all complete actions with valid arguments
                if (message != null && message.getText() != null && message.getText().startsWith("/")) {
                    CommandMessage commandMessage = new CommandMessage(message);
                    telegramMessageCommandValidator.validate(commandMessage);
                    // TODO:  selection tests
                    commandProcessors.get(commandMessage.getCommand()).process(commandMessage);
                } else if (update.hasPollAnswer()) {
                    CommandMessage commandMessage = new CommandMessage(update.getPollAnswer());
                    // TODO:   validate? ;
                    commandProcessors.get(commandMessage.getCommand()).process(commandMessage);
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
            // TODO:  test replyId null case
            MessageDto messageDto = new MessageDto(Long.toString(answerableException.getTelegramChatId()), answerableException.getMessage(), answerableException.getTelegramTopicId(), null);
            messagingService.sendMessageAsync(messageDto);
        } catch (Exception e) {
            // TODO:
            throw new RuntimeException(e);
        }
    }
}
