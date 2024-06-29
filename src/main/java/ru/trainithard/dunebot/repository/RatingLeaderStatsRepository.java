package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.reporting.RatingLeaderStats;

public interface RatingLeaderStatsRepository extends JpaRepository<RatingLeaderStats, Long> {
}
