package ru.trainithard.dunebot.configuration.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.model.scheduler.DuneBotTask;
import ru.trainithard.dunebot.model.scheduler.StateRunnable;
import ru.trainithard.dunebot.model.scheduler.TaskStatus;
import ru.trainithard.dunebot.repository.DunebotTaskRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiFunction;

@Slf4j
@Profile(value = {"prod", "dev"})
@Component
public class DuneBotTaskScheduler extends ThreadPoolTaskScheduler {
    private static final int THREAD_POOL_SIZE = 2;
    private final transient DunebotTaskRepository taskRepository;
    private final transient BiFunction<Runnable, DuneBotTaskId, StateRunnable> stateTaskFactory;
    private final Map<DuneBotTaskId, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public DuneBotTaskScheduler(DunebotTaskRepository taskRepository, Clock clock,
                                BiFunction<Runnable, DuneBotTaskId, StateRunnable> stateTaskFactory) {
        super();
        setPoolSize(THREAD_POOL_SIZE);
        setClock(clock);
        setThreadNamePrefix("dunebot-scheduler");
        this.taskRepository = taskRepository;
        this.stateTaskFactory = stateTaskFactory;
    }

    public ScheduledFuture<?> rescheduleSingleRunTask(Runnable taskRunnable, DuneBotTaskId taskId, Instant startTime) {
        cancel(taskId);
        saveTask(taskId, startTime);

        Runnable stateRunnable = stateTaskFactory.apply(taskRunnable, taskId);
        ScheduledFuture<?> scheduledFeature = schedule(stateRunnable, startTime);
        scheduledTasks.put(taskId, scheduledFeature);
        log.debug("0: rescheduled taskRunnable of type {}", taskId);
        return scheduledFeature;
    }

    private void saveTask(DuneBotTaskId taskId, Instant startTime) {
        DuneBotTask task = taskRepository
                .findByTypeAndEntityIdAndStatus(taskId.getTaskType(), taskId.getEntityId(), TaskStatus.SCHEDULED)
                .orElse(new DuneBotTask(taskId, startTime));
        task.setStatus(TaskStatus.SCHEDULED);
        task.setStartTime(startTime);
        taskRepository.save(task);
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
