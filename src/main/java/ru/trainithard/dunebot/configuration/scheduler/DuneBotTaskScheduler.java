package ru.trainithard.dunebot.configuration.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.model.scheduler.DuneBotTask;
import ru.trainithard.dunebot.model.scheduler.TaskStatus;
import ru.trainithard.dunebot.repository.DunebotTaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class DuneBotTaskScheduler {
    private static final int THREAD_POOL_SIZE = 4;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final DunebotTaskRepository taskRepository;
    private final Map<DuneBotTaskId, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DuneBotTaskScheduler(DunebotTaskRepository taskRepository, Clock clock) {
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(THREAD_POOL_SIZE);
        this.taskScheduler.setClock(clock);
        this.taskScheduler.setThreadNamePrefix("dunebot-scheduler");
        this.taskScheduler.initialize();
        this.taskRepository = taskRepository;
    }

    public ScheduledFuture<?> rescheduleSingleRunTask(Runnable taskRunnable, DuneBotTaskId taskId, Instant startTime) {
        cancel(taskId);

        ScheduledFuture<?> scheduledFeature = taskScheduler.schedule(taskRunnable, startTime);
        scheduledTasks.put(taskId, scheduledFeature);
        log.debug("0: rescheduled taskRunnable of type {}", taskId);

        DuneBotTask task = taskRepository
                .findByTypeAndEntityIdAndStatus(taskId.getTaskType(), taskId.getEntityId(), TaskStatus.SCHEDULED)
                .orElse(new DuneBotTask(taskId, startTime));
        task.setStatus(TaskStatus.SCHEDULED);
        task.setStartTime(startTime);
        taskRepository.save(task);

        return scheduledFeature;
    }

    public void cancelSingleRunTask(DuneBotTaskId taskId) {
        cancel(taskId);
        taskRepository.findByTypeAndEntityIdAndStatus(taskId.getTaskType(), taskId.getEntityId(), TaskStatus.SCHEDULED)
                .ifPresent(task -> {
                    task.setStatus(TaskStatus.CANCELLED);
                    taskRepository.save(task);
                });
    }

    private void cancel(DuneBotTaskId taskId) {
        ScheduledFuture<?> currentTask = scheduledTasks.remove(taskId);
        if (currentTask != null) {
            currentTask.cancel(false);
            log.debug("0: cancelled task of type {}", taskId);
        }
    }

    public ScheduledFuture<?> get(DuneBotTaskId taskId) {
        return scheduledTasks.get(taskId);
    }
}
