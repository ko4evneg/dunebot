package ru.trainithard.dunebot.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerRatingTest {
    private final PlayerRating playerRating = new PlayerRating();
    private final MatchPlayer matchPlayer = new MatchPlayer();
    private final Match match = new Match();

    @BeforeEach
    void beforeEach() {
        match.setState(MatchState.FINISHED);
        match.setFinishDate(LocalDate.of(2010, 10, 30));
        matchPlayer.setMatch(match);
        playerRating.setRatingDate(LocalDate.of(2010, 10, 1));
    }

    @Test
    void shouldNotChangePlayerWhenItExists() {
        Player player = new Player();
        playerRating.setPlayer(player);
        matchPlayer.setLeader(new Leader());
        matchPlayer.setPlace(1);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getPlayer()).isSameAs(player);
    }

    @Test
    void shouldSetPlayerWhenItDoesNotExist() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getPlayer()).isSameAs(player);
    }

    @Test
    void shouldSetCurrentStrikeLengthForNewRatingWhenPlayerWon() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getCurrentStrikeLength()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void shouldNotSetCurrentStrikeLengthForNewRatingWhenPlayerLost(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getCurrentStrikeLength()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldIncreaseCurrentStrikeLengthForExistingRatingWhenPlayerWon(boolean isPreviouslyWon) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        playerRating.setPreviouslyWon(isPreviouslyWon);
        playerRating.setCurrentStrikeLength(4);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getCurrentStrikeLength()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void shouldResetCurrentStrikeLengthForExistingRatingWhenPlayerLost(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);
        playerRating.setCurrentStrikeLength(4);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getCurrentStrikeLength()).isZero();
    }

    @Test
    void shouldSetPreviouslyWonTrueForNewRatingWhenPlayerWon() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.isPreviouslyWon()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void shouldSetPreviouslyWonFalseForNewRatingWhenPlayerLost(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.isPreviouslyWon()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSetPreviouslyWonTrueForExistingRatingWhenPlayerWon(boolean isPreviouslyWon) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        playerRating.setPreviouslyWon(isPreviouslyWon);
        playerRating.setCurrentStrikeLength(4);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.isPreviouslyWon()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void shouldSetPreviouslyWonFalseForExistingRatingWhenPlayerLost(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);
        playerRating.setPreviouslyWon(true);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.isPreviouslyWon()).isFalse();
    }

    @Test
    void shouldSetMaxStrikeLengthOneForNewRatingWhenPlayerWon() {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getMaxStrikeLength()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void shouldSetMaxStrikeLengthZeroForNewRatingWhenPlayerLost(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getMaxStrikeLength()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"0, 0, 1", "2, 2, 3", "5, 5, 6"})
    void shouldIncreaseMaxStrikeLengthForExistingRatingWhenPlayerWonAndHadLesserMaxStrike(
            int currentMaxStrike, int currentStrike, int expectedMaxStrike) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        playerRating.setPreviouslyWon(true);
        playerRating.setCurrentStrikeLength(currentStrike);
        playerRating.setMaxStrikeLength(currentMaxStrike);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getMaxStrikeLength()).isEqualTo(expectedMaxStrike);
    }

    @ParameterizedTest
    @CsvSource({"5, 2, 5", "7, 6, 7"})
    void shouldNotChangeMaxStrikeLengthForExistingRatingWhenPlayerWonAndHadGreaterMaxStrike(
            int currentMaxStrike, int currentStrike, int expectedMaxStrike) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(1);
        playerRating.setPreviouslyWon(true);
        playerRating.setCurrentStrikeLength(currentStrike);
        playerRating.setMaxStrikeLength(currentMaxStrike);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getMaxStrikeLength()).isEqualTo(expectedMaxStrike);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4})
    void shouldNotChangeMaxStrikeLengthToCurrentWhenPlayerLost(int place) {
        Player player = new Player();
        matchPlayer.setPlayer(player);
        matchPlayer.setPlace(place);
        playerRating.setMaxStrikeLength(3);
        playerRating.setCurrentStrikeLength(3);

        playerRating.consume(List.of(matchPlayer));

        assertThat(playerRating.getMaxStrikeLength()).isEqualTo(3);
    }

    @Test
    void shouldAcceptMatchPlayersInCorrectOrder() {
        Player player = new Player();

        Match match1 = new Match();
        match1.setState(MatchState.FINISHED);
        match1.setFinishDate(LocalDate.of(2010, 10, 3));
        MatchPlayer matchPlayer1 = new MatchPlayer();
        matchPlayer1.setPlayer(player);
        matchPlayer1.setPlace(1);
        matchPlayer1.setMatch(match1);
        matchPlayer1.setId(1L);

        Match match2 = new Match();
        match2.setState(MatchState.FINISHED);
        match2.setFinishDate(LocalDate.of(2010, 10, 10));
        MatchPlayer matchPlayer2 = new MatchPlayer();
        matchPlayer2.setPlayer(player);
        matchPlayer2.setPlace(1);
        matchPlayer2.setMatch(match2);
        matchPlayer2.setId(2L);

        Match match3 = new Match();
        match3.setState(MatchState.FINISHED);
        match3.setFinishDate(LocalDate.of(2010, 10, 15));
        MatchPlayer matchPlayer3 = new MatchPlayer();
        matchPlayer3.setPlayer(player);
        matchPlayer3.setPlace(2);
        matchPlayer3.setMatch(match3);
        matchPlayer3.setId(3L);

        Match match4 = new Match();
        match4.setState(MatchState.FINISHED);
        match4.setFinishDate(LocalDate.of(2010, 10, 15));
        MatchPlayer matchPlayer4 = new MatchPlayer();
        matchPlayer4.setPlayer(player);
        matchPlayer4.setPlace(1);
        matchPlayer4.setMatch(match4);
        matchPlayer4.setId(4L);

        playerRating.consume(List.of(matchPlayer4, matchPlayer3, matchPlayer2, matchPlayer1));

        assertThat(playerRating)
                .extracting(PlayerRating::getMaxStrikeLength, PlayerRating::getCurrentStrikeLength)
                .containsExactly(2, 1);
    }
}
