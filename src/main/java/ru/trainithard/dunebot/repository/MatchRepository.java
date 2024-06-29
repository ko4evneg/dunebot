package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;

import java.time.LocalDate;
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
            select m1 from Match m1
            left join fetch m1.matchPlayers mp1
            where m1.id =
                (select m2.id from Match m2
                left join m2.matchPlayers mp2
                where mp2.player.id = :playerId and m2.state in :matchStates
                order by m2.createdAt desc limit 1)
            """)
    Optional<Match> findLatestPlayerMatch(long playerId, Collection<MatchState> matchStates);

    Optional<Match> findByExternalPollIdPollId(String telegramPollId);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            where m.id = :matchId
            """)
    Optional<Match> findWithMatchPlayersBy(long matchId);

    List<Match> findAllByStateIn(Collection<MatchState> states);

    @Query("""
            select m from Match m
            left join fetch m.matchPlayers mp
            left join fetch mp.leader l
            left join fetch mp.player p
            where
            (m.finishDate >= :from and m.finishDate <= :to or
            m.finishDate is null and m.createdAt >= :from and m.createdAt <= :to)
            and m.state in :states""")
    List<Match> findMatchesInPeriod(LocalDate from, LocalDate to, Collection<MatchState> states);
}
