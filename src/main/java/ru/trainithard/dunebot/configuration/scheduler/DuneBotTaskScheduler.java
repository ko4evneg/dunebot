package ru.trainithard.dunebot.configuration.scheduler;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class DuneBotTaskScheduler extends ThreadPoolTaskScheduler {
    private static final int THREAD_POOL_SIZE = 4;
    private final Map<DuneTaskType, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DuneBotTaskScheduler(Clock clock) {
        super();
        this.setPoolSize(THREAD_POOL_SIZE);
        this.setClock(clock);
        this.setThreadNamePrefix("dunebot-scheduler");
    }

    public ScheduledFuture<?> reschedule(Runnable task, DuneTaskType taskType, Instant startTime) {
        cancel(taskType);

        ScheduledFuture<?> scheduledFeature = schedule(task, startTime);
        scheduledTasks.put(taskType, scheduledFeature);
        return scheduledFeature;
    }

    public void cancel(DuneTaskType taskType) {
        ScheduledFuture<?> currentTask = scheduledTasks.get(taskType);
        if (currentTask != null) {
            currentTask.cancel(false);
        }
    }
}
