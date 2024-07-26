package ru.trainithard.dunebot.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderRatingTest {
    private final Leader leader = new Leader();
    private final LeaderRating leaderRating = new LeaderRating(leader, LocalDate.of(2010, 10, 1));
    private final MatchPlayer matchPlayer = new MatchPlayer();

    @BeforeEach
    void beforeEach() {
        Match match = new Match();
        match.setState(MatchState.FINISHED);
        match.setFinishDate(LocalDate.of(2010, 10, 30));
        matchPlayer.setMatch(match);
    }

    @Test
    void shouldNotChangeLeaderWhenItExists() {
        leaderRating.setLeader(leader);
        matchPlayer.setLeader(new Leader());
        matchPlayer.setPlace(1);

        leaderRating.consume(List.of(matchPlayer));

        assertThat(leaderRating.getLeader()).isSameAs(leader);
    }

    @Test
    void shouldSetLeaderWhenItDoesNotExist() {
        matchPlayer.setLeader(leader);
        matchPlayer.setPlace(1);

        leaderRating.consume(List.of(matchPlayer));

        assertThat(leaderRating.getLeader()).isSameAs(leader);
    }
}
