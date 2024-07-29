package ru.trainithard.dunebot.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractRatingTest {
    private static final LocalDate MATCH_FINISH_DATE = LocalDate.of(2010, 10, 30);
    private static final LocalDate RATING_DATE = LocalDate.of(2010, 10, 1);
    private final AbstractRating rating = new PlayerRating();
    private final MatchPlayer matchPlayer = new MatchPlayer();
    private final Match match = new Match();

    @BeforeEach
    void beforeEach() {
        match.setState(MatchState.FINISHED);
        match.setFinishDate(MATCH_FINISH_DATE);
        matchPlayer.setMatch(match);
        rating.setRatingDate(RATING_DATE);
    }

    @Test
    void shouldUpdateRatingDateWhenMatchDateLater() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getRatingDate()).isEqualTo(MATCH_FINISH_DATE);
    }

    @Test
    void shouldUpdateRatingDateWhenMatchDateLaterWithNonRateablePlayer() {
        Player player = new Player();
        matchPlayer.setPlayer(player);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getRatingDate()).isEqualTo(MATCH_FINISH_DATE);
    }

    @Test
    void shouldNotUpdateRatingDateWhenMatchDateEarlier() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        match.setFinishDate(RATING_DATE);
        rating.setRatingDate(MATCH_FINISH_DATE);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getRatingDate()).isEqualTo(MATCH_FINISH_DATE);
    }

    @Test
    void shouldNotProcessNotRateableMatchPlayer() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        rating.setFirstPlaceCount(3);
        rating.setWinRate(1.2);
        rating.setEfficiency(1.3);
        rating.setMatchesCount(3);

        rating.consume(List.of(matchPlayer));

        assertThat(rating)
                .extracting(AbstractRating::getFirstPlaceCount, AbstractRating::getWinRate, AbstractRating::getEfficiency,
                        AbstractRating::getMatchesCount)
                .containsExactly(3, 1.2, 1.3, 3);
    }

    @Test
    void shouldProcessRateableMatchPlayer() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        rating.setFirstPlaceCount(3);
        rating.setWinRate(1.2);
        rating.setEfficiency(1.3);
        rating.setMatchesCount(3);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getFirstPlaceCount())
                .isEqualTo(4);
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"FINISHED"})
    void shouldNotProcessMatchPlayerFromNotFinishedMatch(MatchState state) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        match.setState(state);
        rating.setFirstPlaceCount(3);
        rating.setWinRate(1.2);
        rating.setEfficiency(1.3);
        rating.setMatchesCount(3);

        rating.consume(List.of(matchPlayer));

        assertThat(rating)
                .extracting(AbstractRating::getFirstPlaceCount, AbstractRating::getWinRate, AbstractRating::getEfficiency,
                        AbstractRating::getMatchesCount)
                .containsExactly(3, 1.2, 1.3, 3);
    }

    @Test
    void shouldProcessMatchPlayerFromFinishedMatch() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        match.setState(MatchState.FINISHED);
        rating.setFirstPlaceCount(3);
        rating.setWinRate(1.2);
        rating.setEfficiency(1.3);
        rating.setMatchesCount(3);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getFirstPlaceCount())
                .isEqualTo(4);
    }

    @ParameterizedTest
    @MethodSource("mismatchedDatesSource")
    void shouldNotProcessMatchPlayerFromWrongMonth(LocalDate ratingDate, LocalDate matchDate, String error) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        match.setFinishDate(matchDate);
        rating.setRatingDate(ratingDate);
        rating.setFirstPlaceCount(3);
        rating.setWinRate(1.2);
        rating.setEfficiency(1.3);
        rating.setMatchesCount(3);

        rating.consume(List.of(matchPlayer));

        assertThat(rating)
                .overridingErrorMessage(error)
                .extracting(AbstractRating::getFirstPlaceCount, AbstractRating::getWinRate, AbstractRating::getEfficiency,
                        AbstractRating::getMatchesCount)
                .containsExactly(3, 1.2, 1.3, 3);
    }

    private static Stream<Arguments> mismatchedDatesSource() {
        return Stream.of(
                Arguments.of(LocalDate.of(2010, 4, 1),
                        LocalDate.of(2010, 3, 31), "Closest previous month day should not be accepted"),
                Arguments.of(LocalDate.of(2010, 4, 30),
                        LocalDate.of(2010, 5, 1), "Closest next month day should not be accepted"),
                Arguments.of(LocalDate.of(2010, 4, 30),
                        LocalDate.of(2010, 6, 30), "Other month same day should not be accepted"),
                Arguments.of(LocalDate.of(2010, 1, 1),
                        LocalDate.of(2009, 12, 31), "Previous year closest day should not be accepted"),
                Arguments.of(LocalDate.of(2010, 12, 31),
                        LocalDate.of(2011, 1, 1), "Next year closest day should not be accepted"),
                Arguments.of(LocalDate.of(2010, 4, 1),
                        LocalDate.of(2011, 4, 1), "Other year same day should not be accepted")
        );
    }

    @ParameterizedTest
    @MethodSource("matchedDatesSource")
    void shouldProcessMatchPlayerFromCorrectMonth(LocalDate ratingDate, LocalDate matchDate, String error) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        match.setFinishDate(matchDate);
        rating.setRatingDate(ratingDate);
        rating.setFirstPlaceCount(3);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getFirstPlaceCount())
                .overridingErrorMessage(error)
                .isEqualTo(4);
    }

    private static Stream<Arguments> matchedDatesSource() {
        return Stream.of(
                Arguments.of(LocalDate.of(2010, 4, 1),
                        LocalDate.of(2010, 4, 1), "Same day should be accepted"),
                Arguments.of(LocalDate.of(2010, 4, 1),
                        LocalDate.of(2010, 4, 30), "First and last day should be accepted"),
                Arguments.of(LocalDate.of(2010, 4, 30),
                        LocalDate.of(2010, 4, 1), "Last and first day should be accepted")
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldIncrementMatchesCountOnPlaceAssignmentForNewRating(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getMatchesCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldIncrementMatchesCountOnPlaceAssignmentForExistingRating(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);
        rating.setMatchesCount(5);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getMatchesCount()).isEqualTo(6);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldSetLeaderPlacesForNewRating(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);

        rating.consume(List.of(matchPlayer));

        int actualPlaceCount = getActualRatingPlaceCount(rating, place);
        assertThat(actualPlaceCount).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldSetLeaderPlacesForExistingRating(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);
        rating.setFirstPlaceCount(3);
        rating.setSecondPlaceCount(3);
        rating.setThirdPlaceCount(3);
        rating.setFourthPlaceCount(3);

        rating.consume(List.of(matchPlayer));

        int actualPlaceCount = getActualRatingPlaceCount(rating, place);
        assertThat(actualPlaceCount).isEqualTo(4);
    }

    @ParameterizedTest
    @CsvSource(value = {"1, 1.0", "2, 0.6", "3, 0.4", "4, 0.1"})
    void shouldCalculateEfficiencyForNewRating(int place, double expectedEfficiency) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getEfficiency()).isEqualTo(expectedEfficiency);
    }

    @ParameterizedTest
    @CsvSource(value = {"1, 0.46", "2, 0.43", "3, 0.41", "4, 0.4"})
    void shouldCalculateEfficiencyForExistingRating(int place, double expectedEfficiency) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);
        rating.setMatchesCount(14);
        rating.setFirstPlaceCount(2);
        rating.setSecondPlaceCount(3);
        rating.setThirdPlaceCount(4);
        rating.setFourthPlaceCount(5);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getEfficiency()).isEqualTo(expectedEfficiency);
    }

    @ParameterizedTest
    @CsvSource(value = {"1, 1.0", "2, 0.0", "3, 0.0", "4, 0.0"})
    void shouldCalculateWinRateForNewRating(int place, double expectedWinRate) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getWinRate()).isEqualTo(expectedWinRate);
    }

    @ParameterizedTest
    @CsvSource(value = {"1, 0.2", "2, 0.1", "3, 0.1", "4, 0.1"})
    void shouldCalculateWinRateForExistingRating(int place, double expectedWinRate) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);
        rating.setMatchesCount(9);
        rating.setFirstPlaceCount(1);

        rating.consume(List.of(matchPlayer));

        assertThat(rating.getWinRate()).isEqualTo(expectedWinRate);
    }

    private int getActualRatingPlaceCount(AbstractRating abstractRating, int place) {
        if (place == 1) {
            return abstractRating.getFirstPlaceCount();
        }
        if (place == 2) {
            return abstractRating.getSecondPlaceCount();
        }
        if (place == 3) {
            return abstractRating.getThirdPlaceCount();
        }
        if (place == 4) {
            return abstractRating.getFourthPlaceCount();
        }
        throw new RuntimeException();
    }
}
