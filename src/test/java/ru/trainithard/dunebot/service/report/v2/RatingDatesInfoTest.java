package ru.trainithard.dunebot.service.report.v2;

import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.PlayerRating;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RatingDatesInfoTest {
    @Test
    void shouldSetEarliestRatingDateWhenOneEntityPresented() {
        PlayerRating playerRating1 = createPlayerRating(10000L, LocalDate.of(2010, 3, 12));
        PlayerRating playerRating2 = createPlayerRating(10000L, LocalDate.of(2010, 3, 1));
        PlayerRating playerRating3 = createPlayerRating(10000L, LocalDate.of(2010, 2, 10));
        LocalDate earliestDate = LocalDate.of(2010, 1, 30);
        PlayerRating playerRating4 = createPlayerRating(10000L, earliestDate);
        List<PlayerRating> playerRatings = List.of(playerRating1, playerRating2, playerRating3, playerRating4);

        RatingDatesInfo<PlayerRating> ratingDatesInfo = new RatingDatesInfo<>(playerRatings);

        assertThat(ratingDatesInfo.getEarliestRatingDate()).isEqualTo(earliestDate);
    }

    private PlayerRating createPlayerRating(long playerId, LocalDate date) {
        PlayerRating playerRating = new PlayerRating();
        Player player = new Player();
        player.setId(playerId);
        playerRating.setPlayer(player);
        playerRating.setRatingDate(date);
        return playerRating;
    }

    @Test
    void shouldSetEarliestRatingDateWhenMultipleEntitiesPresented() {
        PlayerRating playerRating1 = createPlayerRating(10000L, LocalDate.of(2010, 3, 12));
        PlayerRating playerRating2 = createPlayerRating(10000L, LocalDate.of(2010, 3, 1));
        PlayerRating playerRating3 = createPlayerRating(10001L, LocalDate.of(2010, 2, 10));
        LocalDate earliestDate = LocalDate.of(2010, 1, 30);
        PlayerRating playerRating4 = createPlayerRating(10001L, earliestDate);
        PlayerRating playerRating5 = createPlayerRating(10002L, LocalDate.of(2010, 4, 10));
        List<PlayerRating> playerRatings = List.of(playerRating1, playerRating2, playerRating3, playerRating4, playerRating5);

        RatingDatesInfo<PlayerRating> ratingDatesInfo = new RatingDatesInfo<>(playerRatings);

        assertThat(ratingDatesInfo.getEarliestRatingDate()).isEqualTo(earliestDate);
    }

    @Test
    void shouldSetEarliestRatingDateWhenSameDatesPresented() {
        LocalDate earliestDate = LocalDate.of(2010, 1, 30);
        PlayerRating playerRating1 = createPlayerRating(10000L, earliestDate);
        PlayerRating playerRating2 = createPlayerRating(10001L, earliestDate);
        PlayerRating playerRating3 = createPlayerRating(10002L, earliestDate);
        List<PlayerRating> playerRatings = List.of(playerRating1, playerRating2, playerRating3);

        RatingDatesInfo<PlayerRating> ratingDatesInfo = new RatingDatesInfo<>(playerRatings);

        assertThat(ratingDatesInfo.getEarliestRatingDate()).isEqualTo(earliestDate);
    }

    @Test
    void shouldSetNullEarliestDateWhenEmptyRatingsCollectionProvided() {
        RatingDatesInfo<PlayerRating> ratingDatesInfo = new RatingDatesInfo<>(Collections.emptyList());

        assertThat(ratingDatesInfo.getEarliestRatingDate()).isNull();
    }

    @Test
    void shouldReturnLatestRatingDateWhenOneEntityPresented() {
        LocalDate latestDate = LocalDate.of(2010, 3, 12);
        PlayerRating playerRating1 = createPlayerRating(10000L, latestDate);
        PlayerRating playerRating2 = createPlayerRating(10000L, LocalDate.of(2010, 3, 1));
        PlayerRating playerRating3 = createPlayerRating(10000L, LocalDate.of(2010, 2, 10));
        PlayerRating playerRating4 = createPlayerRating(10000L, LocalDate.of(2010, 1, 30));
        List<PlayerRating> playerRatings = List.of(playerRating1, playerRating2, playerRating3, playerRating4);

        RatingDatesInfo<PlayerRating> ratingDatesInfo = new RatingDatesInfo<>(playerRatings);

        assertThat(ratingDatesInfo.getLatestRatingsById(10000L).orElseThrow().getRatingDate()).isEqualTo(latestDate);
    }

    @Test
    void shouldReturnLatestRatingDateWhenMultipleEntitiesPresented() {
        LocalDate latestDate10000 = LocalDate.of(2010, 3, 12);
        PlayerRating playerRating1 = createPlayerRating(10000L, latestDate10000);
        PlayerRating playerRating2 = createPlayerRating(10000L, LocalDate.of(2010, 3, 1));
        LocalDate latestDate10001 = LocalDate.of(2010, 2, 10);
        PlayerRating playerRating3 = createPlayerRating(10001L, latestDate10001);
        PlayerRating playerRating4 = createPlayerRating(10001L, LocalDate.of(2010, 1, 30));
        LocalDate latestDate10002 = LocalDate.of(2010, 4, 10);
        PlayerRating playerRating5 = createPlayerRating(10002L, latestDate10002);
        List<PlayerRating> playerRatings = List.of(playerRating1, playerRating2, playerRating3, playerRating4, playerRating5);

        RatingDatesInfo<PlayerRating> ratingDatesInfo = new RatingDatesInfo<>(playerRatings);

        assertThat(ratingDatesInfo.getLatestRatingsById(10000L).orElseThrow().getRatingDate()).isEqualTo(latestDate10000);
        assertThat(ratingDatesInfo.getLatestRatingsById(10001L).orElseThrow().getRatingDate()).isEqualTo(latestDate10001);
        assertThat(ratingDatesInfo.getLatestRatingsById(10002L).orElseThrow().getRatingDate()).isEqualTo(latestDate10002);
    }

    @Test
    void shouldReturnLatestRatingDateWhenSameDatesPresented() {
        LocalDate latestDate = LocalDate.of(2010, 1, 30);
        PlayerRating playerRating1 = createPlayerRating(10000L, latestDate);
        PlayerRating playerRating2 = createPlayerRating(10001L, latestDate);
        PlayerRating playerRating3 = createPlayerRating(10002L, latestDate);
        PlayerRating playerRating4 = createPlayerRating(10002L, latestDate);
        List<PlayerRating> playerRatings = List.of(playerRating1, playerRating2, playerRating3, playerRating4);

        RatingDatesInfo<PlayerRating> ratingDatesInfo = new RatingDatesInfo<>(playerRatings);

        assertThat(ratingDatesInfo.getLatestRatingsById(10000L).orElseThrow().getRatingDate()).isEqualTo(latestDate);
        assertThat(ratingDatesInfo.getLatestRatingsById(10001L).orElseThrow().getRatingDate()).isEqualTo(latestDate);
        assertThat(ratingDatesInfo.getLatestRatingsById(10002L).orElseThrow().getRatingDate()).isEqualTo(latestDate);
    }

    @Test
    void shouldReturnNullLatestDateWhenEmptyRatingsCollectionProvided() {
        RatingDatesInfo<PlayerRating> ratingDatesInfo = new RatingDatesInfo<>(Collections.emptyList());

        assertThat(ratingDatesInfo.getLatestRatingsById(10000L).isEmpty()).isTrue();
    }
}
