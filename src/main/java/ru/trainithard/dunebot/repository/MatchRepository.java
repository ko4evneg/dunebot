package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.trainithard.dunebot.model.Leader;
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
    List<Match> findLatestPlayerMatch(long externalPlayerId, Collection<MatchState> matchStates);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where m.id = (select mp2.match.id from MatchPlayer mp2 where mp2.match.state = :matchState and mp2.player.externalId = :externalPlayerId)
            order by m.createdAt desc
            limit 1
            """)
    List<Match> findLatestPlayerMatchWithMatchPlayerBy(long externalPlayerId, MatchState matchState);

    Optional<Match> findByExternalPollIdPollId(String telegramPollId);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where m.id = :matchId
            """)
    Optional<Match> findWithMatchPlayersBy(long matchId);

    List<Match> findAllByStateNotIn(Collection<MatchState> states);

    @Modifying
    @Transactional
    @Query("update Match m set m.leaderWon = :leader where m.id = :matchId")
    void saveLeader(long matchId, Leader leader);
}
