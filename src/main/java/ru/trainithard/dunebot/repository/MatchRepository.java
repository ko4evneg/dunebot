package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
