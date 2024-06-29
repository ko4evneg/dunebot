package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.reporting.RatingMatchStats;

import java.time.LocalDate;

public interface RatingMatchStatsRepository extends JpaRepository<RatingMatchStats, Long> {
    @Query("select max(r.endDate) from RatingMatchStats r")
    LocalDate findLatestEndDate();
}
