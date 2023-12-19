package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.Match;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where mp.player.telegramId = :telegramPlayerId
            order by m.createdAt desc limit 1
            """)
    Optional<Match> findLatestOwnedMatch(long telegramPlayerId);
}
