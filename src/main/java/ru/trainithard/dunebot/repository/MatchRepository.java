package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query("""
            select m from Match m
            left join fetch m.matchPlayers
            where m.owner.id = :playerId
            order by m.createdAt desc limit 1
            """)
    Optional<Match> findLatestOwnedMatchWithMatchPlayersBy(long playerId);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where mp.player.externalId = :externalPlayerId and m.state in :matchStates
            """)
    List<Match> findPlayerMatches(long externalPlayerId, Collection<MatchState> matchStates);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where mp.player.id = :playerId and m.state in :matchStates
            order by m.createdAt desc limit 1
            """)
    Optional<Match> findLatestPlayerMatch(long playerId, Collection<MatchState> matchStates);

    Optional<Match> findByExternalPollIdPollId(String telegramPollId);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where m.id = :matchId
            """)
    Optional<Match> findWithMatchPlayersBy(long matchId);

    List<Match> findAllByStateNotIn(Collection<MatchState> states);
}
