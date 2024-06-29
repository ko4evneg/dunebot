package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.reporting.RatingMatchStats;

public interface RatingMatchStatsRepository extends JpaRepository<RatingMatchStats, Long> {
}
