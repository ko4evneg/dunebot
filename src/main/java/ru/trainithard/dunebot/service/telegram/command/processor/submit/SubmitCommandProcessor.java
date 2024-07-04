package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.SubmitValidatedMatchRetriever;
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
    private static final int RESUBMIT_TIME_LIMIT_STEP = 60 * 7;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final DuneBotTaskScheduler taskScheduler;
    private final SubmitValidatedMatchRetriever validatedMatchRetriever;
    private final AppSettingsService appSettingsService;
    private final Clock clock;
    private final DuneScheduledTaskFactory taskFactory;
    private final ExternalMessageFactory messageFactory;
    private final KeyboardsFactory keyboardsFactory;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: SUBMIT started", logId());
        Match validatedMatch = validatedMatchRetriever.getValidatedSubmitMatch(commandMessage);
        process(validatedMatch, commandMessage.getChatId());
        log.debug("{}: SUBMIT ended", logId());
    }

    void process(Match match, Long chatId) {
        log.debug("{}: SUBMIT(internal) started", logId());

        if (match.getState() == MatchState.NEW) {
            match.setState(MatchState.ON_SUBMIT);
            matchRepository.save(match);
            log.debug("{}: match {} saved state ON_SUBMIT", logId(), match.getId());
        } else if (match.getState() == MatchState.SUBMITTED) {
            resetMatchAndPlayersSubmitState(match);
        }

        ExternalMessage submitMessage = messageFactory.getPlayersSubmitMessage(match.getId());
        List<List<ButtonDto>> submitPlayersKeyboard = keyboardsFactory.getSubmitPlayersKeyboard(match.getMatchPlayers());
        MessageDto submitPlayersMessage = new MessageDto(chatId, submitMessage, null, submitPlayersKeyboard);
        messagingService.sendMessageAsync(submitPlayersMessage);
        rescheduleFinishTasks(match.getId());

        log.debug("{}: SUBMIT(internal) ended", logId());
    }

    private void resetMatchAndPlayersSubmitState(Match match) {
        match.prepareForResubmit();
        match.getMatchPlayers().forEach(matchPlayer -> {
            matchPlayer.setPlace(null);
            matchPlayer.setLeader(null);
        });
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(match.getMatchPlayers());
        });
        log.debug("{}: match {} and its players prepared for resubmit and receive ON_SUBMIT state", logId(), match.getId());
    }

    private void rescheduleFinishTasks(long matchId) {
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
