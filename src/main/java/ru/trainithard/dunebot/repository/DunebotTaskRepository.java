package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.scheduler.DuneBotTask;
import ru.trainithard.dunebot.model.scheduler.TaskStatus;

import java.util.Optional;

public interface DunebotTaskRepository extends JpaRepository<DuneBotTask, Long> {
    @Query("select t from DuneBotTask t " +
           "where t.duneBotTaskId.taskType = :taskType and t.duneBotTaskId.entityId = :entityId and t.status = :status")
    Optional<DuneBotTask> findByTypeAndEntityIdAndStatus(DuneTaskType taskType, Long entityId, TaskStatus status);
}
