package ru.trainithard.dunebot.service.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class RatingReportTest {
    private final List<MatchPlayer> matchPlayers = new ArrayList<>();

    @BeforeEach
    void beforeEach() {
        Match match1 = getMatch(1);
        matchPlayers.add(getMatchPlayer(match1, 1, 1));
        matchPlayers.add(getMatchPlayer(match1, 2, 2));
        matchPlayers.add(getMatchPlayer(match1, 3, 3));
        matchPlayers.add(getMatchPlayer(match1, 4, 4));
        Match match2 = getMatch(2);
        matchPlayers.add(getMatchPlayer(match2, 1, 4));
        matchPlayers.add(getMatchPlayer(match2, 2, 3));
        matchPlayers.add(getMatchPlayer(match2, 3, 2));
        matchPlayers.add(getMatchPlayer(match2, 4, 1));
        Match match3 = getMatch(3);
        matchPlayers.add(getMatchPlayer(match3, 1, 1));
        matchPlayers.add(getMatchPlayer(match3, 2, 3));
        matchPlayers.add(getMatchPlayer(match3, 3, 4));
        matchPlayers.add(getMatchPlayer(match3, 4, 2));
        Match match4 = getMatch(4);
        matchPlayers.add(getMatchPlayer(match4, 5, 3));
        matchPlayers.add(getMatchPlayer(match4, 1, 2));
        matchPlayers.add(getMatchPlayer(match4, 2, 4));
        matchPlayers.add(getMatchPlayer(match4, 3, 1));
        matchPlayers.add(getMatchPlayer(match4, 6, 0));
        Match match5 = getMatch(5);
        matchPlayers.add(getMatchPlayer(match5, 5, 2));
        matchPlayers.add(getMatchPlayer(match5, 6, 1));
        matchPlayers.add(getMatchPlayer(match5, 4, 4));
        matchPlayers.add(getMatchPlayer(match5, 1, 3));
        Match match6 = getMatch(6);
        matchPlayers.add(getMatchPlayer(match6, 5, 2));
        matchPlayers.add(getMatchPlayer(match6, 6, 3));
        matchPlayers.add(getMatchPlayer(match6, 3, 4));
        matchPlayers.add(getMatchPlayer(match6, 1, 3));
        matchPlayers.add(getMatchPlayer(match6, 7, 0));
    }

    @Test
    void shouldCountAllPlayerMatches() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getMatchesCount()).isEqualTo(6);
    }

    @Test
    void shouldNotThrowOnNullPlace() {
        matchPlayers.add(getMatchPlayer(getMatch(7), 7, null));

        assertThatCode(() -> new RatingReport(matchPlayers, ModType.CLASSIC, 15))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldReturnEachPlayerMatchesCounts() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerRatings())
                .extracting(RatingReport.PlayerMonthlyRating::getPlayerFriendlyName, RatingReport.PlayerMonthlyRating::getMatchesCount)
                .containsExactlyInAnyOrder(
                        tuple("f6 (s6) l6", 2L),
                        tuple("f1 (s1) l1", 6L),
                        tuple("f4 (s4) l4", 4L),
                        tuple("f3 (s3) l3", 5L),
                        tuple("f2 (s2) l2", 4L),
                        tuple("f5 (s5) l5", 3L)
                );
    }


    @Test
    void shouldReturnEachPlayerWinRates() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerRatings())
                .extracting(RatingReport.PlayerMonthlyRating::getPlayerFriendlyName, RatingReport.PlayerMonthlyRating::getWinRate)
                .containsExactlyInAnyOrder(
                        tuple("f6 (s6) l6", 50.0),
                        tuple("f1 (s1) l1", 33.33),
                        tuple("f4 (s4) l4", 25.0),
                        tuple("f3 (s3) l3", 20.0),
                        tuple("f2 (s2) l2", 0.0),
                        tuple("f5 (s5) l5", 0.0)
                );
    }

    @Test
    void shouldReturnEachPlayerEfficiencies() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 5);

        assertThat(monthlyRating.getPlayerRatings())
                .extracting(RatingReport.PlayerMonthlyRating::getPlayerFriendlyName, RatingReport.PlayerMonthlyRating::getEfficiency)
                .containsExactlyInAnyOrder(
                        tuple("f6 (s6) l6", 0.7),
                        tuple("f1 (s1) l1", 0.58),
                        tuple("f4 (s4) l4", 0.45),
                        tuple("f3 (s3) l3", 0.44),
                        tuple("f2 (s2) l2", 0.37),
                        tuple("f5 (s5) l5", 0.53)
                );
    }

    @Test
    void shouldReturnCorrectlyOrderedPlayers() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 1);

        assertThat(monthlyRating.getPlayerRatings())
                .extracting(RatingReport.PlayerMonthlyRating::getPlayerFriendlyName)
                .containsExactly("f6 (s6) l6", "f1 (s1) l1", "f5 (s5) l5", "f4 (s4) l4", "f3 (s3) l3", "f2 (s2) l2");
    }

    @Test
    void shouldPutPlayersOverRatingThresholdOnTop() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 5);

        assertThat(monthlyRating.getPlayerRatings())
                .extracting(RatingReport.PlayerMonthlyRating::getPlayerFriendlyName)
                .containsExactly("f1 (s1) l1", "f3 (s3) l3", "f6 (s6) l6", "f5 (s5) l5", "f4 (s4) l4", "f2 (s2) l2");
    }

    @Test
    void shouldSetZeroPlacesCountForMissingPlaces() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        Set<RatingReport.PlayerMonthlyRating> actualPlayerRatings = monthlyRating.getPlayerRatings();

        assertThat(actualPlayerRatings)
                .filteredOn(playerMonthlyRating -> "f6 (s6) l6".equals(playerMonthlyRating.getPlayerFriendlyName()))
                .extracting(RatingReport.PlayerMonthlyRating::getOrderedPlaceCountByPlaceNames)
                .flatExtracting(Map::values)
                .contains(1L, 0L, 1L, 0L);
    }

    @Test
    void shouldExcludeZeroPlacesFromRating() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        Map<Integer, Long> player6Places = monthlyRating.getPlayerRatings().stream()
                .filter(playerMonthlyRating -> playerMonthlyRating.getPlayerFriendlyName().equals("f6 (s6) l6"))
                .findFirst().orElseThrow()
                .getOrderedPlaceCountByPlaceNames();

        assertThat(player6Places).hasSize(4);
    }

    @Test
    void shouldNotConsiderNoWinsPlayersInRating() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        boolean isPlayer7Present = monthlyRating.getPlayerRatings().stream()
                .anyMatch(playerMonthlyRating -> playerMonthlyRating.getPlayerFriendlyName().equals("f7 (s7) l7"));

        assertThat(isPlayer7Present).isFalse();
    }

    @Test
    void shouldExcludeGuestPlayersFromRating() {
        matchPlayers.stream()
                .map(MatchPlayer::getPlayer)
                .filter(player -> Set.of("s5", "s6").contains(player.getSteamName()))
                .forEach(player -> player.setGuest(true));

        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerRatings())
                .extracting(RatingReport.PlayerMonthlyRating::getPlayerFriendlyName, RatingReport.PlayerMonthlyRating::getMatchesCount)
                .containsExactlyInAnyOrder(
                        tuple("f1 (s1) l1", 6L),
                        tuple("f4 (s4) l4", 4L),
                        tuple("f3 (s3) l3", 5L),
                        tuple("f2 (s2) l2", 4L)
                );
    }

    private MatchPlayer getMatchPlayer(Match match, int playerId, Integer place) {
        MatchPlayer matchPlayer = new MatchPlayer(match, getPlayer(playerId));
        matchPlayer.setPlace(place);
        return matchPlayer;
    }

    private Match getMatch(int matchId) {
        Match match = new Match();
        match.setId((long) matchId);
        return match;
    }

    private Player getPlayer(int playerId) {
        Player player = new Player();
        player.setId((long) playerId);
        player.setFirstName("f" + playerId);
        player.setSteamName("s" + playerId);
        player.setLastName("l" + playerId);
        return player;
    }
}
