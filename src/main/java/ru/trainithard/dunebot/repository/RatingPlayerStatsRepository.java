package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.reporting.RatingPlayerStats;

public interface RatingPlayerStatsRepository extends JpaRepository<RatingPlayerStats, Long> {
}
