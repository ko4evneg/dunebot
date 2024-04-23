package ru.trainithard.dunebot.service.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.service.LogId;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;
import ru.trainithard.dunebot.service.telegram.factory.CommandMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.CommandProcessorFactory;
import ru.trainithard.dunebot.service.telegram.factory.ValidationStrategyFactory;
import ru.trainithard.dunebot.service.telegram.validator.CommonCommandMessageValidator;
import ru.trainithard.dunebot.service.telegram.validator.ValidationStrategy;

/**
 * Core class reponsible for polling and parsing of external messages queue, detecting, validating and processing
 * parsed commands.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateProcessor {
    private final TelegramBot telegramBot;
    private final MessagingService messagingService;
    private final CommandMessageFactory commandMessageFactory;
    private final ValidationStrategyFactory validationStrategyFactory;
    private final CommandProcessorFactory commandProcessorFactory;
    private final CommonCommandMessageValidator commonCommandMessageValidator;

    public void process() {
        Update update = telegramBot.poll();
        while (update != null) {
            LogId.init();
            int logId = LogId.get();
            try {
                CommandMessage commandMessage = commandMessageFactory.getInstance(update);
                if (commandMessage != null) {
                    log.debug("{}: received {} command from {}", logId, commandMessage.getCommand(), commandMessage.getUserId());

                    commonCommandMessageValidator.validate(commandMessage);
                    log.debug("{}: successfully passed common validation", logId);

                    ValidationStrategy validator = validationStrategyFactory.getValidator(commandMessage.getCommand().getCommandType());
                    validator.validate(commandMessage);
                    log.debug("{}: successfully passed specific validation", logId);

                    CommandProcessor processor = commandProcessorFactory.getProcessor(commandMessage.getCommand());
                    processor.process(commandMessage);
                    log.debug("{}: successfully processed", logId);
                }
            } catch (AnswerableDuneBotException answerableException) {
                sendAnswerableExceptionMessage(answerableException, logId);
            } catch (Exception exception) {
                log.error(logId + ": command failed due to an exception", exception);
            } finally {
                LogId.clear();
                update = telegramBot.poll();
            }
        }
    }

    private void sendAnswerableExceptionMessage(AnswerableDuneBotException answerableException, int logId) {
        messagingService.sendMessageAsync(new MessageDto(answerableException));
        log.error("{}: command failed due to app-specific exception. {}", logId, answerableException.getMessage());
        if (answerableException.getCause() != null) {
            log.error(logId + ":", answerableException);
        }
    }
}
