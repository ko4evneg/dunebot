package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.model.Player;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByTelegramId(long telegramId);

    Optional<Player> findByTelegramIdOrSteamName(long telegramId, @Nullable String steamName);
}
