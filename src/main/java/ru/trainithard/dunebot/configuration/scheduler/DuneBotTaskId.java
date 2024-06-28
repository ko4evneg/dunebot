package ru.trainithard.dunebot.configuration.scheduler;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
public final class DuneBotTaskId {
    @Enumerated(EnumType.STRING)
    private DuneTaskType taskType;
    private Long entityId;

    public DuneBotTaskId(DuneTaskType taskType, Long entityId) {
        this.taskType = taskType;
        this.entityId = entityId;
    }

    public DuneBotTaskId(DuneTaskType taskType) {
        this.taskType = taskType;
    }
}
