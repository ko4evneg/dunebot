package ru.trainithard.dunebot.service.report.v2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.repository.LeaderRatingRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRatingRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RatingServiceTest {
    private final Clock clock = mock(Clock.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final PlayerRatingRepository playerRatingRepository = mock(PlayerRatingRepository.class);
    private final LeaderRatingRepository leaderRatingRepository = mock(LeaderRatingRepository.class);
    private final RatingMergeService<PlayerRating> playerRatingMergeService = mock(RatingMergeService.class);
    private final RatingMergeService<LeaderRating> leaderRatingMergeService = mock(RatingMergeService.class);
    private final RatingService ratingService = new RatingService(clock, matchRepository, playerRatingRepository,
            leaderRatingRepository, playerRatingMergeService, leaderRatingMergeService);
    private final Match match1 = new Match();
    private final Match match2 = new Match();
    private final Match match3 = new Match();
    private final PlayerRating playerRating1 = new PlayerRating(new Player(), date(3, 7));
    private final PlayerRating playerRating2 = new PlayerRating(new Player(), date(3, 4));
    private final LeaderRating leaderRating1 = new LeaderRating(new Leader(), date(3, 5));
    private final LeaderRating leaderRating2 = new LeaderRating(new Leader(), date(3, 2));
    private final LocalDate TODAY = date(10, 1);

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(TODAY.atTime(LocalTime.of(1, 0)).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        match1.setFinishDate(date(4, 15));
        match2.setFinishDate(date(5, 10));
        match3.setFinishDate(date(6, 17));
    }

    @Test
    void shouldSelectFromNextDayOfOfSameRatingsEarliestDate() {
        doReturn(List.of(playerRating1, playerRating2)).when(playerRatingRepository).findLatestPlayerRatings();

        ratingService.buildFullRating();

        verify(matchRepository).findAllByDatesAndState(eq(date(3, 5)), any(), any());
    }

    @Test
    void shouldSelectFromNextDayOfOfDifferentRatingsEarliestDate() {
        doReturn(List.of(playerRating1, playerRating2)).when(playerRatingRepository).findLatestPlayerRatings();
        doReturn(List.of(leaderRating1, leaderRating2)).when(leaderRatingRepository).findLatestLeaderRatings();

        ratingService.buildFullRating();

        verify(matchRepository).findAllByDatesAndState(eq(date(3, 3)), any(), any());
    }

    @Test
    void shouldSelectFromMatchEarliestDateWhenNoRatings() {
        doReturn(date(7, 8)).when(matchRepository).findEarliestFinishDate();

        ratingService.buildFullRating();

        verify(matchRepository).findAllByDatesAndState(eq(date(7, 8)), any(), any());
    }

    @Test
    void shouldSelectToDateToday() {
        ratingService.buildFullRating();

        verify(matchRepository).findAllByDatesAndState(any(), any(), argThat((List<MatchState> states) ->
                states.size() == 1 && states.get(0) == MatchState.FINISHED));
    }

    @Test
    void shouldSelectOnlyFinishedMatches() {
        ratingService.buildFullRating();

        verify(matchRepository).findAllByDatesAndState(any(), eq(TODAY), any());
    }

    @Test
    void shouldPassAllPlayerRatingsToPLayerUpdateService() {
        doReturn(List.of(playerRating1, playerRating2)).when(playerRatingRepository).findLatestPlayerRatings();

        ratingService.buildFullRating();

        ArgumentCaptor<List<PlayerRating>> playerRatingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingMergeService).updateRatings(any(), playerRatingsCaptor.capture());
        List<PlayerRating> actualRatings = playerRatingsCaptor.getValue();

        assertThat(actualRatings).containsExactlyInAnyOrder(playerRating1, playerRating2);
    }

    @Test
    void shouldPassAllLeaderRatingsToPLayerUpdateService() {
        doReturn(List.of(leaderRating1, leaderRating2)).when(leaderRatingRepository).findLatestLeaderRatings();

        ratingService.buildFullRating();

        ArgumentCaptor<List<LeaderRating>> leadersRatingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(leaderRatingMergeService).updateRatings(any(), leadersRatingsCaptor.capture());
        List<LeaderRating> actualRatings = leadersRatingsCaptor.getValue();

        assertThat(actualRatings).containsExactlyInAnyOrder(leaderRating1, leaderRating2);
    }

    @Test
    void shouldPassAllMatchesToMergeServices() {
        doReturn(List.of(match1, match2, match3)).when(matchRepository).findAllByDatesAndState(any(), any(), any());

        ratingService.buildFullRating();

        ArgumentCaptor<List<Match>> playerMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Match>> leaderMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingMergeService).updateRatings(playerMergeMatchesCaptor.capture(), any());
        verify(leaderRatingMergeService).updateRatings(leaderMergeMatchesCaptor.capture(), any());
        List<Match> actualPlayerMatches = playerMergeMatchesCaptor.getValue();
        List<Match> actualLeaderMatches = leaderMergeMatchesCaptor.getValue();

        assertThat(actualPlayerMatches).containsExactlyInAnyOrder(match1, match2, match3);
        assertThat(actualLeaderMatches).containsExactlyInAnyOrder(match1, match2, match3);
    }

    private LocalDate date(int month, int day) {
        return LocalDate.of(2010, month, day);
    }
}
