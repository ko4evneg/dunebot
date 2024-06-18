package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.scheduler.DunebotTask;

public interface DunebotTaskRepository extends JpaRepository<DunebotTask, Long> {
}
