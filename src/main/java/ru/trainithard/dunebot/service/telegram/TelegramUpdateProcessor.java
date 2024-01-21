package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;
import ru.trainithard.dunebot.service.telegram.factory.CommandMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.CommandProcessorFactory;
import ru.trainithard.dunebot.service.telegram.factory.ValidationStrategyFactory;
import ru.trainithard.dunebot.service.telegram.validator.ValidationStrategy;

@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
    private final TelegramBot telegramBot;
    private final MessagingService messagingService;
    private final CommandMessageFactory commandMessageFactory;
    private final ValidationStrategyFactory validationStrategyFactory;
    private final CommandProcessorFactory commandProcessorFactory;

    public void process() {
        Update update = telegramBot.poll();
        while (update != null) {
            try {
                CommandMessage commandMessage = commandMessageFactory.getInstance(update);
                if (commandMessage != null) {
                    validateCommand(commandMessage);
                    ValidationStrategy validator = validationStrategyFactory.getValidator(commandMessage.getCommand().getCommandType());
                    validator.validate(commandMessage);
                    CommandProcessor processor = commandProcessorFactory.getProcessor(commandMessage.getCommand());
                    processor.process(commandMessage);
                }
            } catch (AnswerableDuneBotException answerableException) {
                sendUserNotificationMessage(answerableException);
            } catch (Exception ignored) {
            } finally {
                update = telegramBot.poll();
            }
        }
    }

    private void validateCommand(CommandMessage commandMessage) {
        if (commandMessage.getCommand() == null) {
            throw new AnswerableDuneBotException("Неверная команда!", commandMessage);
        }
    }

    private void sendUserNotificationMessage(AnswerableDuneBotException answerableException) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(answerableException.getTelegramChatId());
            sendMessage.setText(answerableException.getMessage());
            MessageDto messageDto = new MessageDto(Long.toString(answerableException.getTelegramChatId()), answerableException.getMessage(), answerableException.getTelegramReplyId(), null);
            messagingService.sendMessageAsync(messageDto);
        } catch (Exception ignored) {
        }
    }
}
