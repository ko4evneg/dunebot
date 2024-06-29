package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.reporting.RatingLeaderStats;

import java.time.LocalDate;

public interface RatingLeaderStatsRepository extends JpaRepository<RatingLeaderStats, Long> {
    @Query("select max(r.endDate) from RatingLeaderStats r")
    LocalDate findLatestEndDate();
}
