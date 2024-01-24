package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where mp.player.id = :playerId
            order by m.createdAt desc limit 1
            """)
    Optional<Match> findLatestOwnedMatch(long playerId);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where mp.player.externalId = :externalPlayerId and m.state = :matchState
            """)
    List<Match> findLatestPlayerMatch(long externalPlayerId, MatchState matchState);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where m.state = :matchState and m.id = (select mp2.match.id from MatchPlayer mp2 where mp2.player.externalId = :externalPlayerId)
            """)
    List<Match> findLatestPlayerMatchWithMatchPlayerBy(long externalPlayerId, MatchState matchState);

    Optional<Match> findByExternalPollIdPollId(String telegramPollId);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where m.id = :matchId
            """)
    Optional<Match> findWithMatchPlayersBy(long matchId);
}
