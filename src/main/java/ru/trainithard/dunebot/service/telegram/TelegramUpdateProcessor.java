package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Core class reponsible for polling and parsing of external messages queue, detecting, validating and processing
 * parsed commands.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
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
            int loggingId = random.nextInt(0, Integer.MAX_VALUE);
            try {
                CommandMessage commandMessage = commandMessageFactory.getInstance(update);
                if (commandMessage != null) {
                    log.debug("{}: received {} command from {}", loggingId, commandMessage.getCommand(), commandMessage.getUserId());

                    commonCommandMessageValidator.validate(commandMessage);
                    log.debug("{}: successfully passed common validation", loggingId);

                    ValidationStrategy validator = validationStrategyFactory.getValidator(commandMessage.getCommand().getCommandType());
                    validator.validate(commandMessage);
                    log.debug("{}: successfully passed specific validation", loggingId);

                    CommandProcessor processor = commandProcessorFactory.getProcessor(commandMessage.getCommand());
                    processor.process(commandMessage, loggingId);
                    log.debug("{}: successfully processed", loggingId);
                }
            } catch (AnswerableDuneBotException answerableException) {
                messagingService.sendMessageAsync(new MessageDto(answerableException));
                log.error(loggingId + ": command failed due to app-specific exception", answerableException);
            } catch (Exception exception) {
                log.error(loggingId + ": command failed due to an exception", exception);
            } finally {
                update = telegramBot.poll();
            }
        }
    }
}
