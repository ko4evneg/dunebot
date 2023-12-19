package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.MatchPlayer;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long> {

}
