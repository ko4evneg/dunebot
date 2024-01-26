package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;
import ru.trainithard.dunebot.service.telegram.factory.CommandMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.CommandProcessorFactory;
import ru.trainithard.dunebot.service.telegram.factory.ValidationStrategyFactory;
import ru.trainithard.dunebot.service.telegram.validator.CommonCommandMessageValidator;
import ru.trainithard.dunebot.service.telegram.validator.ValidationStrategy;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TelegramUpdateProcessor.class);
    private static final Random random = new Random();

    private final TelegramBot telegramBot;
    private final MessagingService messagingService;
    private final CommandMessageFactory commandMessageFactory;
    private final ValidationStrategyFactory validationStrategyFactory;
    private final CommandProcessorFactory commandProcessorFactory;
    private final CommonCommandMessageValidator commonCommandMessageValidator;

    public void process() {
        Update update = telegramBot.poll();
        while (update != null) {
            long loggingId = random.nextLong();
            try {
                CommandMessage commandMessage = commandMessageFactory.getInstance(update);
                if (commandMessage != null) {
                    logger.debug("{}: received {} command from {}", loggingId, commandMessage.getCommand(), commandMessage.getUserId());

                    commonCommandMessageValidator.validate(commandMessage);
                    logger.debug("{}: successfully passed common validation", loggingId);

                    ValidationStrategy validator = validationStrategyFactory.getValidator(commandMessage.getCommand().getCommandType());
                    validator.validate(commandMessage);
                    logger.debug("{}: successfully passed specific validation", loggingId);

                    CommandProcessor processor = commandProcessorFactory.getProcessor(commandMessage.getCommand());
                    processor.process(commandMessage);
                    logger.debug("{}: successfully processed", loggingId);
                }
            } catch (AnswerableDuneBotException answerableException) {
                messagingService.sendMessageAsync(new MessageDto(answerableException));
                logger.error(loggingId + ": command failed due to app-specific exception", answerableException);
            } catch (Exception exception) {
                logger.error(loggingId + ": command failed due to an exception", exception);
            } finally {
                update = telegramBot.poll();
            }
        }
    }
}
