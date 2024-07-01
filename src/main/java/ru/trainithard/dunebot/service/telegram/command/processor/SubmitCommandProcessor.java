package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
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
        int logId = logId();
        log.debug("{}: SUBMIT(internal) started", logId);

        if (match.getState() == MatchState.NEW) {
            match.setState(MatchState.ON_SUBMIT);
            matchRepository.save(match);
            log.debug("{}: match {} saved state ON_SUBMIT", logId, match.getId());
        }

        ExternalMessage submitMessage = messageFactory.getSubmitMessage(match.getId());
        List<List<ButtonDto>> submitPlayersKeyboard = keyboardsFactory.getSubmitPlayersKeyboard(match.getMatchPlayers());
        MessageDto submitPlayersMessage = new MessageDto(chatId, submitMessage, null, submitPlayersKeyboard);
        messagingService.sendMessageAsync(submitPlayersMessage);
        rescheduleForcedFailFinish(match.getId());

        log.debug("{}: SUBMIT(internal) ended", logId);
    }

    private void rescheduleForcedFailFinish(long matchId) {
        int finishMatchTimeout = appSettingsService.getIntSetting(AppSettingKey.FINISH_MATCH_TIMEOUT);
        Instant forcedFinishTime = Instant.now(clock).plus(finishMatchTimeout, ChronoUnit.MINUTES);
        DuneBotTaskId submitTimeoutTaskId = new DuneBotTaskId(DuneTaskType.SUBMIT_TIMEOUT, matchId);
        ScheduledFuture<?> oldFailFinishTask = taskScheduler.get(submitTimeoutTaskId);
        if (oldFailFinishTask != null) {
            long delay = oldFailFinishTask.getDelay(TimeUnit.SECONDS);
            forcedFinishTime = Instant.now(clock).plus(RESUBMIT_TIME_LIMIT_STEP + delay, ChronoUnit.SECONDS);
        }
        DunebotRunnable submitTimeoutTask = taskFactory.createInstance(submitTimeoutTaskId);
        taskScheduler.rescheduleSingleRunTask(submitTimeoutTask, submitTimeoutTaskId, forcedFinishTime);
    }

    @Override
    public Command getCommand() {
        return Command.SUBMIT;
    }
}
