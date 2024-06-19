package ru.trainithard.dunebot.model.scheduler;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.model.BaseEntity;

import java.time.Instant;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "DUNEBOT_TASKS")
@Getter
@Setter
@NoArgsConstructor
public class DuneBotTask extends BaseEntity {
    @Embedded
    protected DuneBotTaskId duneBotTaskId;
    protected Instant startTime;
    @Enumerated(EnumType.STRING)
    protected TaskStatus status;

    public DuneBotTask(DuneBotTaskId duneBotTaskId, Instant startTime) {
        this.duneBotTaskId = duneBotTaskId;
        this.startTime = startTime;
        this.status = TaskStatus.SCHEDULED;
    }
}
