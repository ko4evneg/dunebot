package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.PlayerMatch;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatch, Long> {
}
