package ru.trainithard.dunebot.service.report.v2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.repository.PlayerRatingRepository;
import ru.trainithard.dunebot.service.MetaDataService;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

class RatingUpdateServiceTest {
    private Match match1 = getMatch(date(1, 3), 10000, 10001, 10002, 10003);
    private Match match2 = getMatch(date(1, 5), 10001, 10000, 10003, 10002);
    private Match match3 = getMatch(date(1, 10), 10003, 10002, 10000, 10001);
    private final Player player1 = new Player();
    private final Player player2 = new Player();
    private final Player player3 = new Player();
    private final Player player4 = new Player();
    private final PlayerRating rating1 = new PlayerRating(player1, date(1, 3));
    private final PlayerRating rating2 = new PlayerRating(player2, date(1, 5));
    private final PlayerRating rating3 = new PlayerRating(player3, date(1, 7));
    private final PlayerRating rating4 = new PlayerRating(player4, date(1, 10));
    private final PlayerRatingRepository playerRatingRepository = mock(PlayerRatingRepository.class);
    private final RatingUpdateService<PlayerRating> ratingUpdateService = new PlayerRatingUpdateService(playerRatingRepository);
    private final MetaDataService metaDataService = mock(MetaDataService.class);

    @BeforeEach
    void beforeEach() throws ReflectiveOperationException {
        Field field1 = ratingUpdateService.getClass().getSuperclass().getDeclaredField("transactionTemplate");
        Field field2 = ratingUpdateService.getClass().getSuperclass().getDeclaredField("metaDataService");
        field1.setAccessible(true);
        field2.setAccessible(true);
        field1.set(ratingUpdateService, new MockTransactionTemplate());
        field2.set(ratingUpdateService, metaDataService);

        player1.setId(10000L);
        player2.setId(10001L);
        player3.setId(10002L);
        player4.setId(10003L);
    }

    @Test
    void shouldSaveNewRatingsForSingleMonthWhenNoRatingsProvided() {
        ratingUpdateService.updateRatings(List.of(match1, match2, match3), Collections.emptyList());

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        LocalDate expectedRatingDate = date(1, 10);

        assertThat(actualRatings)
                .hasSize(4)
                .extracting(playerRating -> playerRating.getPlayer().getId(), PlayerRating::getEfficiency, PlayerRating::getRatingDate)
                .containsExactlyInAnyOrder(
                        tuple(10000L, 0.66, expectedRatingDate), tuple(10001L, 0.56, expectedRatingDate),
                        tuple(10002L, 0.36, expectedRatingDate), tuple(10003L, 0.5, expectedRatingDate)
                );
    }

    @Test
    void shouldSaveNewRatingsForMultipleMonthsWhenNoRatingsProvided() {
        LocalDate janDate = date(1, 3);
        LocalDate febDate = date(2, 3);
        LocalDate marchDate = date(3, 3);
        match1 = getMatch(janDate, 10000, 10001, 10002, 10003);
        match2 = getMatch(febDate, 10001, 10000, 10003, 10002);
        match3 = getMatch(marchDate, 10003, 10002, 10000, 10001);

        ratingUpdateService.updateRatings(List.of(match1, match2, match3), Collections.emptyList());

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository, times(3)).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        assertThat(actualRatings)
                .hasSize(12)
                .extracting(playerRating -> playerRating.getPlayer().getId(), PlayerRating::getEfficiency, PlayerRating::getRatingDate)
                .containsExactlyInAnyOrder(
                        tuple(10000L, 1.0, janDate), tuple(10001L, 0.6, janDate),
                        tuple(10002L, 0.4, janDate), tuple(10003L, 0.1, janDate),
                        tuple(10000L, 0.6, febDate), tuple(10001L, 1.0, febDate),
                        tuple(10002L, 0.1, febDate), tuple(10003L, 0.4, febDate),
                        tuple(10000L, 0.4, marchDate), tuple(10001L, 0.1, marchDate),
                        tuple(10002L, 0.6, marchDate), tuple(10003L, 1.0, marchDate)
                );
    }

    @Test
    void shouldNotSaveRatingForNonParticipantWhenNoRatingsProvided() {
        match1 = getMatch(date(1, 3), 10000, 10001, 10002, 10003, 10004);
        match1.getMatchPlayers().stream().filter(matchPlayer -> matchPlayer.getPlayer().getId().equals(10004L))
                .forEach(matchPlayer -> matchPlayer.setPlace(0));

        ratingUpdateService.updateRatings(List.of(match1, match2, match3), Collections.emptyList());

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        LocalDate expectedRatingDate = date(1, 10);
        assertThat(actualRatings)
                .hasSize(4)
                .extracting(playerRating -> playerRating.getPlayer().getId(), PlayerRating::getEfficiency, PlayerRating::getRatingDate)
                .containsExactlyInAnyOrder(
                        tuple(10000L, 0.66, expectedRatingDate), tuple(10001L, 0.56, expectedRatingDate),
                        tuple(10002L, 0.36, expectedRatingDate), tuple(10003L, 0.5, expectedRatingDate)
                );
    }

    @Test
    void shouldNotSaveNewRatingsForSingleWhenNoMatchesProvided() {
        ratingUpdateService.updateRatings(Collections.emptyList(), List.of(rating1, rating2, rating3, rating4));

        verifyNoInteractions(playerRatingRepository);
    }

    @Test
    void shouldNotSaveNewRatingsForMultipleMonthWhenNoMatchesProvided() {
        ratingUpdateService.updateRatings(Collections.emptyList(), List.of(rating1, rating2, rating3, rating4));
        rating2.setRatingDate(date(2, 3));
        rating3.setRatingDate(date(3, 3));
        rating4.setRatingDate(date(4, 3));

        verifyNoInteractions(playerRatingRepository);
    }

    @Test
    void shouldNotSaveRatingWhenItsDateEarlierOrEqualToMatchDateInSameMonth() {
        ratingUpdateService.updateRatings(List.of(match1, match2, match3), List.of(rating1, rating2, rating3, rating4));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        assertThat(actualRatings)
                .hasSize(3)
                .extracting(PlayerRating::getEntityId)
                .doesNotContain(10003L);
    }

    @Test
    void shouldNotSaveRatingWhenItsDateEarlierOrEqualToMatchDateInMultipleMonth() {
        match2.setFinishDate(date(2, 1));
        match3.setFinishDate(date(2, 10));
        rating3.setRatingDate(date(2, 10));
        PlayerRating rating3jan = new PlayerRating(player3, date(1, 10));
        rating4.setRatingDate(date(2, 10));
        PlayerRating rating4jan = new PlayerRating(player4, date(1, 10));

        ratingUpdateService.updateRatings(List.of(match1, match2, match3), List.of(rating1, rating2, rating3, rating4, rating3jan, rating4jan));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        assertThat(actualRatings)
                .hasSize(2)
                .extracting(PlayerRating::getEntityId)
                .doesNotContain(10003L, 10002L);
    }

    @Test
    void shouldSaveRatingWhenItsDateIsAfterMatchDateInSameMonth() {
        rating1.setMatchesCount(3);
        rating2.setMatchesCount(3);
        rating3.setMatchesCount(3);
        ratingUpdateService.updateRatings(List.of(match1, match2, match3), List.of(rating1, rating2, rating3, rating4));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        assertThat(actualRatings)
                .hasSize(3)
                .extracting(PlayerRating::getEntityId, PlayerRating::getRatingDate, PlayerRating::getMatchesCount)
                .containsExactlyInAnyOrder(
                        tuple(10000L, date(1, 10), 5),
                        tuple(10001L, date(1, 10), 4),
                        tuple(10002L, date(1, 10), 4)
                );
    }

    @Test
    void shouldSaveRatingWhenItsDateIsAfterMatchDateInMultipleMonth() {
        match1.setFinishDate(date(1, 15));
        match2.setFinishDate(date(1, 15));
        match3.setFinishDate(date(2, 15));
        rating4.setRatingDate(date(2, 10));
        PlayerRating rating4jan = new PlayerRating(player4, date(1, 20));

        ratingUpdateService.updateRatings(List.of(match1, match2, match3), List.of(rating1, rating2, rating3, rating4, rating4jan));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository, times(2)).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        assertThat(actualRatings)
                .hasSize(7)
                .extracting(PlayerRating::getEntityId, PlayerRating::getRatingDate, PlayerRating::getMatchesCount)
                .containsExactlyInAnyOrder(
                        tuple(10000L, date(1, 15), 2),
                        tuple(10001L, date(1, 15), 2),
                        tuple(10002L, date(1, 15), 2),
                        tuple(10000L, date(2, 15), 1),
                        tuple(10001L, date(2, 15), 1),
                        tuple(10002L, date(2, 15), 1),
                        tuple(10003L, date(2, 15), 1)
                );
    }

    @Test
    void shouldCountOnlyLaterMatchesToRating() {
        rating4.setRatingDate(date(1, 5));
        rating4.setMatchesCount(2);
        rating4.setEfficiency(0.25);
        rating4.setThirdPlaceCount(1);
        rating4.setFourthPlaceCount(1);

        ratingUpdateService.updateRatings(List.of(match1, match2, match3), List.of(rating1, rating2, rating3, rating4));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        assertThat(actualRatings)
                .extracting(PlayerRating::getEntityId, PlayerRating::getRatingDate, PlayerRating::getMatchesCount,
                        PlayerRating::getEfficiency, PlayerRating::getWinRate)
                .contains(tuple(10003L, date(1, 10), 3, 0.5, 0.3333333333333333));
    }

    @Test
    void shouldSplitRatingsAndMatchesByMonth() {
        rating4.setRatingDate(date(1, 1));
        match3.setFinishDate(date(2, 10));

        ratingUpdateService.updateRatings(List.of(match1, match2, match3), List.of(rating1, rating2, rating3, rating4));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerRatingRepository, times(2)).saveAll(ratingsCaptor.capture());
        List<PlayerRating> actualRatings = ratingsCaptor.getAllValues().stream().flatMap(Collection::stream).toList();

        assertThat(actualRatings)
                .extracting(PlayerRating::getEntityId, PlayerRating::getRatingDate, PlayerRating::getMatchesCount)
                .contains(tuple(10003L, date(1, 5), 2), tuple(10003L, date(2, 10), 1));
    }

    @Test
    void shouldSaveMetaDataKeyForRatings() {
        rating4.setRatingDate(date(1, 1));
        match3.setFinishDate(date(2, 10));

        ratingUpdateService.updateRatings(List.of(match1, match2, match3), List.of(rating1, rating2, rating3, rating4));

        verify(metaDataService).saveOnlyLatestRatingDate(MetaDataKey.PLAYER_RATING_DATE, date(1, 1));
        verify(metaDataService).saveOnlyLatestRatingDate(MetaDataKey.PLAYER_RATING_DATE, date(2, 1));
    }

    private LocalDate date(int month, int day) {
        return LocalDate.of(2010, month, day);
    }

    private Match getMatch(LocalDate finishDate, int... winnerIds) {
        Match match = new Match();
        match.setFinishDate(finishDate);
        match.setState(MatchState.FINISHED);
        List<MatchPlayer> matchPlayers = new ArrayList<>();
        for (int place = 1; place <= winnerIds.length; place++) {
            MatchPlayer matchPlayer = new MatchPlayer();
            Player player = new Player();
            long winnerId = winnerIds[place - 1];
            player.setId(winnerId);
            matchPlayer.setId(winnerId);
            matchPlayer.setPlayer(player);
            matchPlayer.setPlace(place);
            matchPlayer.setMatch(match);
            matchPlayers.add(matchPlayer);
        }
        match.setMatchPlayers(matchPlayers);
        return match;
    }

    private static class MockTransactionTemplate extends TransactionTemplate {
        @Override
        public void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
            action.accept(mock(TransactionStatus.class));
        }
    }
}
