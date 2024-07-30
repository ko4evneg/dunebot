package ru.trainithard.dunebot.service.report.v2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.repository.LeaderRatingRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRatingRepository;
import ru.trainithard.dunebot.service.MetaDataService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collection;
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
    private final RatingUpdateService<PlayerRating> playerRatingUpdateService = mock(RatingUpdateService.class);
    private final RatingUpdateService<LeaderRating> leaderRatingUpdateService = mock(RatingUpdateService.class);
    private final MetaDataService metaDataService = mock(MetaDataService.class);
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final RatingService ratingService = new RatingService(clock, matchRepository, playerRatingRepository,
            leaderRatingRepository, playerRatingUpdateService, leaderRatingUpdateService, metaDataService, cacheManager);
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

        doReturn(List.of(match1)).when(matchRepository).findAllByDatesAndState(any(), any(), any());
        doReturn(date(3, 1)).when(metaDataService).findRatingDate(any());
    }

    @Test
    void shouldSelectFromNextDayOfMetaDataDate_players() {
        doReturn(List.of(playerRating1, playerRating2)).when(playerRatingRepository).findLatestPlayerRatings();

        ratingService.buildFullRating();

        verify(matchRepository).findAllByDatesAndState(eq(date(3, 2)), any(), any());
    }

    @Test
    void shouldSelectFromNextDayOfMetaDataDate_leaders() {
        doReturn(List.of(playerRating1, playerRating2)).when(playerRatingRepository).findLatestPlayerRatings();
        doReturn(List.of(leaderRating1, leaderRating2)).when(leaderRatingRepository).findLatestLeaderRatings();

        ratingService.buildFullRating();

        verify(matchRepository).findAllByDatesAndState(eq(date(3, 2)), any(), any());
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
    void shouldPassAllPlayerRatingsToPlayerUpdateService() {
        doReturn(List.of(playerRating1, playerRating2)).when(playerRatingRepository).findLatestPlayerRatings();
        doReturn(List.of(match1)).when(matchRepository).findAllByDatesAndState(any(), any(), any());

        ratingService.buildFullRating();

        ArgumentCaptor<List<PlayerRating>> playerRatingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingUpdateService).updateRatings(any(), playerRatingsCaptor.capture(), any());
        List<PlayerRating> actualRatings = playerRatingsCaptor.getValue();

        assertThat(actualRatings).containsExactlyInAnyOrder(playerRating1, playerRating2);
    }

    @Test
    void shouldPassAllLeaderRatingsToPlayerUpdateService() {
        doReturn(List.of(leaderRating1, leaderRating2)).when(leaderRatingRepository).findLatestLeaderRatings();
        doReturn(List.of(match1)).when(matchRepository).findAllByDatesAndState(any(), any(), any());

        ratingService.buildFullRating();

        ArgumentCaptor<List<LeaderRating>> leadersRatingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(leaderRatingUpdateService).updateRatings(any(), leadersRatingsCaptor.capture(), any());
        List<LeaderRating> actualRatings = leadersRatingsCaptor.getValue();

        assertThat(actualRatings).containsExactlyInAnyOrder(leaderRating1, leaderRating2);
    }

    @Test
    void shouldPassAllMatchesToMergeServices() {
        doReturn(List.of(match1, match2, match3)).when(matchRepository).findAllByDatesAndState(any(), any(), any());

        ratingService.buildFullRating();

        ArgumentCaptor<List<Match>> playerMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Match>> leaderMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingUpdateService).updateRatings(playerMergeMatchesCaptor.capture(), any(), any());
        verify(leaderRatingUpdateService).updateRatings(leaderMergeMatchesCaptor.capture(), any(), any());
        List<Match> actualPlayerMatches = playerMergeMatchesCaptor.getValue();
        List<Match> actualLeaderMatches = leaderMergeMatchesCaptor.getValue();

        assertThat(actualPlayerMatches).containsExactlyInAnyOrder(match1, match2, match3);
        assertThat(actualLeaderMatches).containsExactlyInAnyOrder(match1, match2, match3);
    }

    @Test
    void shouldPassOnlyValidMatchesForPlayersWhenPlayerRatingDateIsBeforeLeaderRatingDate() {
        doReturn(List.of(match1, match2, match3)).when(matchRepository).findAllByDatesAndState(any(), any(), any());

        doReturn(date(4, 1)).when(metaDataService).findRatingDate(MetaDataKey.PLAYER_RATING_DATE);
        doReturn(date(5, 10)).when(metaDataService).findRatingDate(MetaDataKey.LEADER_RATING_DATE);

        ratingService.buildFullRating();

        ArgumentCaptor<List<Match>> playerMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Match>> leaderMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingUpdateService).updateRatings(playerMergeMatchesCaptor.capture(), any(), any());
        verify(leaderRatingUpdateService).updateRatings(leaderMergeMatchesCaptor.capture(), any(), any());
        List<Match> actualPlayerMatches = playerMergeMatchesCaptor.getValue();
        List<Match> actualLeaderMatches = leaderMergeMatchesCaptor.getValue();

        assertThat(actualPlayerMatches)
                .hasSize(3)
                .extracting(Match::getFinishDate)
                .containsExactly(date(4, 15), date(5, 10), date(6, 17));

        assertThat(actualLeaderMatches)
                .hasSize(1)
                .extracting(Match::getFinishDate)
                .containsExactly(date(6, 17));
    }

    @Test
    void shouldPassOnlyValidMatchesForLeadersWhenLeaderRatingDateIsBeforePlayerRatingDate() {
        doReturn(List.of(match1, match2, match3)).when(matchRepository).findAllByDatesAndState(any(), any(), any());

        doReturn(date(4, 1)).when(metaDataService).findRatingDate(MetaDataKey.LEADER_RATING_DATE);
        doReturn(date(5, 10)).when(metaDataService).findRatingDate(MetaDataKey.PLAYER_RATING_DATE);

        ratingService.buildFullRating();

        ArgumentCaptor<List<Match>> playerMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Match>> leaderMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingUpdateService).updateRatings(playerMergeMatchesCaptor.capture(), any(), any());
        verify(leaderRatingUpdateService).updateRatings(leaderMergeMatchesCaptor.capture(), any(), any());
        List<Match> actualPlayerMatches = playerMergeMatchesCaptor.getValue();
        List<Match> actualLeaderMatches = leaderMergeMatchesCaptor.getValue();

        assertThat(actualLeaderMatches)
                .hasSize(3)
                .extracting(Match::getFinishDate)
                .containsExactly(date(4, 15), date(5, 10), date(6, 17));

        assertThat(actualPlayerMatches)
                .hasSize(1)
                .extracting(Match::getFinishDate)
                .containsExactly(date(6, 17));
    }

    @Test
    void shouldSelectMatchesOnNextDayFromLastMetaDataDate() {
        ratingService.buildFullRating();

        verify(matchRepository).findAllByDatesAndState(eq(date(3, 2)), any(), any());
    }

    @Test
    void shouldPassMatchesOnNextDayFromLastMetaDataDate() {
        doReturn(date(4, 15)).when(metaDataService).findRatingDate(any());
        doReturn(List.of(match1, match2, match3)).when(matchRepository).findAllByDatesAndState(any(), any(), any());

        ratingService.buildFullRating();

        ArgumentCaptor<List<Match>> playerMergeMatchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingUpdateService).updateRatings(playerMergeMatchesCaptor.capture(), any(), any());
        List<LocalDate> actualMatchDates = playerMergeMatchesCaptor.getAllValues().stream()
                .flatMap(Collection::stream)
                .map(Match::getFinishDate)
                .toList();

        assertThat(actualMatchDates)
                .hasSize(2)
                .doesNotContain(date(4, 15));
    }

    @Test
    void shouldClearRatingsCacheOnCompletion() {
        Cache cache = mock(Cache.class);
        doReturn(cache).when(cacheManager).getCache("playerRatings");

        ratingService.buildFullRating();

        verify(cache).clear();
    }

    private LocalDate date(int month, int day) {
        return LocalDate.of(2025, month, day);
    }
}
