package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.reporting.RatingPlayerStats;

import java.time.LocalDate;

public interface RatingPlayerStatsRepository extends JpaRepository<RatingPlayerStats, Long> {
    @Query("select max(r.endDate) from RatingPlayerStats r")
    LocalDate findLatestEndDate();
}
