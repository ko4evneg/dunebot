package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.trainithard.dunebot.model.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}
