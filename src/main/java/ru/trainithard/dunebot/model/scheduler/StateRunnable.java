package ru.trainithard.dunebot.model.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.exception.MissingScheduledTaskException;
import ru.trainithard.dunebot.repository.DunebotTaskRepository;

@Slf4j
@RequiredArgsConstructor
public class StateRunnable implements Runnable {
    private final DunebotTaskRepository taskRepository;
    private final DuneBotTaskId taskId;
    private final Runnable originalRunnable;

    @Override
    public void run() {
        DuneBotTask task = taskRepository
                .findByTypeAndEntityIdAndStatus(taskId.getTaskType(), taskId.getEntityId(), TaskStatus.SCHEDULED)
                .orElseThrow(() -> new MissingScheduledTaskException("Task can't be run as it is missing in database or already run"));
        task.setStatus(TaskStatus.RUN);
        taskRepository.save(task);

        try {
            log.info("Task {} of type {} execution...", task.getId(), taskId.getTaskType());
            originalRunnable.run();
            log.info("Task {} has been successfully finished", task.getId());
        } catch (Exception e) {
            log.error("Task " + task.getId() + " failed due to an exception.", e);
            task.setStatus(TaskStatus.FAILED);
            taskRepository.save(task);
            return;
        }

        task.setStatus(TaskStatus.FINISHED);
        taskRepository.save(task);
    }
}
