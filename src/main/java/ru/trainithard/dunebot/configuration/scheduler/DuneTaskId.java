package ru.trainithard.dunebot.configuration.scheduler;

import lombok.Data;

@Data
public final class DuneTaskId {
    private final DuneTaskType taskType;
    private final long callerId;

    public DuneTaskId(DuneTaskType taskType, Long callerId) {
        this.taskType = taskType;
        this.callerId = callerId;
    }

    public DuneTaskId(DuneTaskType taskType) {
        this.taskType = taskType;
        this.callerId = -1;
    }
}
