package ru.trainithard.dunebot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long> {
    Optional<MatchPlayer> findByMatchExternalPollIdPollIdAndPlayerExternalId(String externalPollId, long externalUserId);

    @Query("""
            select mp from MatchPlayer mp
            join fetch mp.player p
            where mp.match.finishDate between :from and :to
            and mp.match.state = :matchState
            and mp.match.modType = :modType""")
    List<MatchPlayer> findByMatchDates(LocalDate from, LocalDate to, MatchState matchState, ModType modType);

    List<MatchPlayer> findByMatch(Match match);

    List<MatchPlayer> findByPlayerExternalIdAndMatchState(long externalId, MatchState matchState);
}
