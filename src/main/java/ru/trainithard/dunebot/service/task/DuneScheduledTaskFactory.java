package ru.trainithard.dunebot.service.task;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskId;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class DuneScheduledTaskFactory {
    private final Function<Long, StartMatchTask> startMatchTaskFactory;
    private final Function<Long, SubmitTimeoutTask> submitTimeoutTaskFactory;
    private final ShutdownTask shutdownTask;

    public DunebotRunnable createInstance(DuneTaskId duneTaskId) {
        Long entityId = duneTaskId.getEntityId();
        return switch (duneTaskId.getTaskType()) {
            case START_MESSAGE -> startMatchTaskFactory.apply(entityId);
            case SUBMIT_TIMEOUT -> submitTimeoutTaskFactory.apply(entityId);
            case SHUTDOWN -> shutdownTask;
        };
    }
}
