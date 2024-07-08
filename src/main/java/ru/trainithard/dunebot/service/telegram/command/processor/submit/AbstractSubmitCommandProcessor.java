package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import org.springframework.beans.factory.annotation.Autowired;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.task.DuneScheduledTaskFactory;
import ru.trainithard.dunebot.service.task.DunebotRunnable;
import ru.trainithard.dunebot.service.telegram.command.processor.CommandProcessor;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.service.telegram.factory.messaging.KeyboardsFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

abstract class AbstractSubmitCommandProcessor extends CommandProcessor {
    private static final int RESUBMIT_TIME_LIMIT_STEP = 60 * 7;
    @Autowired
    private DuneBotTaskScheduler taskScheduler;
    @Autowired
    private AppSettingsService appSettingsService;
    @Autowired
    private Clock clock;
    @Autowired
    private DuneScheduledTaskFactory taskFactory;
    @Autowired
    private ExternalMessageFactory messageFactory;
    @Autowired
    private KeyboardsFactory keyboardsFactory;

    void sendSubmitMessages(Match match, long chatId) {
        ExternalMessage submitMessage = messageFactory.getPlayersSubmitMessage(match.getId());
        List<List<ButtonDto>> submitPlayersKeyboard = keyboardsFactory.getSubmitPlayersKeyboard(match.getMatchPlayers());
        MessageDto submitPlayersMessage = new MessageDto(chatId, submitMessage, null, submitPlayersKeyboard);
        messagingService.sendMessageAsync(submitPlayersMessage);
    }

    void rescheduleSubmitTasks(long matchId) {
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
}
