package ru.trainithard.dunebot.service.task;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class DuneScheduledTaskFactory {
    private final Function<Long, StartMatchTask> startMatchTaskFactory;
    private final Function<Long, SubmitTimeoutTask> submitTimeoutTaskFactory;
    private final Function<Long, SubmitTimeoutNotificationTask> submitTimeoutNotificationTaskFactory;
    private final ShutdownTask shutdownTask;

    public DunebotRunnable createInstance(DuneBotTaskId duneBotTaskId) {
        Long entityId = duneBotTaskId.getEntityId();
        return switch (duneBotTaskId.getTaskType()) {
            case START_MESSAGE -> startMatchTaskFactory.apply(entityId);
            case SUBMIT_TIMEOUT -> submitTimeoutTaskFactory.apply(entityId);
            case SUBMIT_TIMEOUT_NOTIFICATION -> submitTimeoutNotificationTaskFactory.apply(entityId);
            case SHUTDOWN -> shutdownTask;
        };
    }
}
