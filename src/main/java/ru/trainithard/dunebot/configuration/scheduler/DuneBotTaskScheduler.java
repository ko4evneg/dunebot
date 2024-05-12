package ru.trainithard.dunebot.configuration.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
public class DuneBotTaskScheduler extends ThreadPoolTaskScheduler {
    private static final int THREAD_POOL_SIZE = 4;
    private final Map<DuneTaskId, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DuneBotTaskScheduler(Clock clock) {
        super();
        this.setPoolSize(THREAD_POOL_SIZE);
        this.setClock(clock);
        this.setThreadNamePrefix("dunebot-scheduler");
    }

    public ScheduledFuture<?> reschedule(Runnable task, DuneTaskId taskId, Instant startTime) {
        cancel(taskId);

        ScheduledFuture<?> scheduledFeature = schedule(task, startTime);
        scheduledTasks.put(taskId, scheduledFeature);
        log.debug("0: rescheduled task of type {}", taskId);

        return scheduledFeature;
    }

    public void cancel(DuneTaskId taskId) {
        ScheduledFuture<?> currentTask = scheduledTasks.remove(taskId);
        if (currentTask != null) {
            currentTask.cancel(false);
            log.debug("0: cancelled task of type {}", taskId);
        }
    }

    public ScheduledFuture<?> get(DuneTaskId taskId) {
        return scheduledTasks.get(taskId);
    }
}
