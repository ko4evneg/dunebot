package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.model.Player;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByExternalId(long externalId);

    Optional<Player> findByExternalIdOrSteamName(long externalId, @Nullable String steamName);

    @Query("select (count(p) > 0) from Player p where p.externalId = :externalId")
    boolean existsByTelegramId(Long externalId);


}
