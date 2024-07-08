package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.MatchNotExistsException;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.task.DuneScheduledTaskFactory;
import ru.trainithard.dunebot.service.task.DunebotRunnable;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.messaging.KeyboardsFactory;
import ru.trainithard.dunebot.service.telegram.validator.SubmitMatchValidator;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Initiates first match results requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitCommandProcessor extends CommandProcessor {
    private static final String MATCH_NOT_EXISTS_EXCEPTION = "Матча с таким ID не существует!";
    private static final int RESUBMIT_TIME_LIMIT_STEP = 60 * 7;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final DuneBotTaskScheduler taskScheduler;
    private final SubmitMatchValidator submitMatchValidator;
    private final AppSettingsService appSettingsService;
    private final Clock clock;
    private final DuneScheduledTaskFactory taskFactory;
    private final ExternalMessageFactory messageFactory;
    private final KeyboardsFactory keyboardsFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: SUBMIT started", logId());
        try {
            long matchId = Long.parseLong(commandMessage.getArgument(1));
            Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow(MatchNotExistsException::new);
            submitMatchValidator.validateSubmitMatch(commandMessage, match);
            Long chatId = commandMessage.getChatId();
            log.debug("{}: SUBMIT(internal) started", logId());

            Player submitter = playerRepository.findByExternalId(chatId).orElseThrow();
            match.setState(MatchState.ON_SUBMIT);
            match.setSubmitter(submitter);
            matchRepository.save(match);
            log.debug("{}: match {} saved state ON_SUBMIT", logId(), match.getId());

            ExternalMessage submitMessage = messageFactory.getPlayersSubmitMessage(match.getId());
            List<List<ButtonDto>> submitPlayersKeyboard = keyboardsFactory.getSubmitPlayersKeyboard(match.getMatchPlayers());
            MessageDto submitPlayersMessage = new MessageDto(chatId, submitMessage, null, submitPlayersKeyboard);
            messagingService.sendMessageAsync(submitPlayersMessage);
            rescheduleSubmitTasks(match.getId());

            log.debug("{}: SUBMIT(internal) ended", logId());
        } catch (NumberFormatException | MatchNotExistsException exception) {
            throw new AnswerableDuneBotException(MATCH_NOT_EXISTS_EXCEPTION, exception, commandMessage.getChatId());
        }
        log.debug("{}: SUBMIT ended", logId());
    }

    private void rescheduleSubmitTasks(long matchId) {
        int finishMatchTimeout = appSettingsService.getIntSetting(AppSettingKey.SUBMIT_TIMEOUT);
        Instant forcedFinishTime = Instant.now(clock).plus(finishMatchTimeout, ChronoUnit.MINUTES);
        DuneBotTaskId submitTimeoutTaskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, matchId);
        rescheduleForcedFailFinish(submitTimeoutTaskId, forcedFinishTime);

        int submitMatchNotificationTimeOffset = appSettingsService.getIntSetting(AppSettingKey.SUBMIT_TIMEOUT_WARNING_NOTIFICATION);
        Instant submitTimeoutMatchNotificationTime = forcedFinishTime.minus(submitMatchNotificationTimeOffset, ChronoUnit.MINUTES);
        DuneBotTaskId submitTimeoutMatchNotificationTaskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT_NOTIFICATION, matchId);
        rescheduleForcedFailFinish(submitTimeoutMatchNotificationTaskId, submitTimeoutMatchNotificationTime);
    }

    private void rescheduleForcedFailFinish(DuneBotTaskId submitTimeoutTaskId, Instant forcedFinishTime) {
        Instant startTime = forcedFinishTime;
        ScheduledFuture<?> oldFailFinishTask = taskScheduler.get(submitTimeoutTaskId);
        if (oldFailFinishTask != null) {
            long delay = oldFailFinishTask.getDelay(TimeUnit.SECONDS);
            startTime = Instant.now(clock).plus(RESUBMIT_TIME_LIMIT_STEP + delay, ChronoUnit.SECONDS);
        }
        DunebotRunnable submitTimeoutTask = taskFactory.createInstance(submitTimeoutTaskId);
        taskScheduler.rescheduleSingleRunTask(submitTimeoutTask, submitTimeoutTaskId, startTime);
    }

    @Override
    public Command getCommand() {
        return Command.SUBMIT;
    }
}
