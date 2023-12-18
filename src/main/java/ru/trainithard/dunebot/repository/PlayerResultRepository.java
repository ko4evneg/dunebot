package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.PlayerResult;

public interface PlayerResultRepository extends JpaRepository<PlayerResult, Long> {
}
