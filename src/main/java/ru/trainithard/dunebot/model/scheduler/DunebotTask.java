package ru.trainithard.dunebot.model.scheduler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskId;
import ru.trainithard.dunebot.model.BaseEntity;

import java.time.Instant;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "DUNEBOT_TASKS")
@Getter
@Setter
@NoArgsConstructor
public class DunebotTask extends BaseEntity {
    @Embedded
    protected DuneTaskId duneTaskId;
    protected Instant startTime;
    @Enumerated(EnumType.STRING)
    protected TaskStatus status;

    public DunebotTask(DuneTaskId duneTaskId, Instant startTime) {
        this.duneTaskId = duneTaskId;
        this.startTime = startTime;
        this.status = TaskStatus.SCHEDULED;
    }
}
