package ru.trainithard.dunebot.configuration.scheduler;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
public final class DuneTaskId {
    private DuneTaskType taskType;
    private Long entityId;

    public DuneTaskId(DuneTaskType taskType, Long entityId) {
        this.taskType = taskType;
        this.entityId = entityId;
    }

    public DuneTaskId(DuneTaskType taskType) {
        this.taskType = taskType;
    }
}
