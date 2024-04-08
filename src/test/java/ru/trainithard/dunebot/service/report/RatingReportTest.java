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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

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

        assertEquals(6, monthlyRating.getMatchesCount());
    }

    @Test
    void shouldNotThrowOnNullPlace() {
        matchPlayers.add(getMatchPlayer(getMatch(7), 7, null));

        assertDoesNotThrow(() -> new RatingReport(matchPlayers, ModType.CLASSIC, 15));
    }

    @Test
    void shouldReturnEachPlayerMatchesCounts() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerRatings(), containsInAnyOrder(
                both(hasProperty("playerFriendlyName", is("f6 (s6) l6"))).and(hasProperty("matchesCount", is(2L))),
                both(hasProperty("playerFriendlyName", is("f1 (s1) l1"))).and(hasProperty("matchesCount", is(6L))),
                both(hasProperty("playerFriendlyName", is("f4 (s4) l4"))).and(hasProperty("matchesCount", is(4L))),
                both(hasProperty("playerFriendlyName", is("f3 (s3) l3"))).and(hasProperty("matchesCount", is(5L))),
                both(hasProperty("playerFriendlyName", is("f2 (s2) l2"))).and(hasProperty("matchesCount", is(4L))),
                both(hasProperty("playerFriendlyName", is("f5 (s5) l5"))).and(hasProperty("matchesCount", is(3L)))
        ));
    }

    @Test
    void shouldReturnEachPlayerWinRates() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerRatings(), containsInAnyOrder(
                both(hasProperty("playerFriendlyName", is("f6 (s6) l6"))).and(hasProperty("winRate", is(50.0))),
                both(hasProperty("playerFriendlyName", is("f1 (s1) l1"))).and(hasProperty("winRate", is(33.33))),
                both(hasProperty("playerFriendlyName", is("f4 (s4) l4"))).and(hasProperty("winRate", is(25.0))),
                both(hasProperty("playerFriendlyName", is("f3 (s3) l3"))).and(hasProperty("winRate", is(20.0))),
                both(hasProperty("playerFriendlyName", is("f2 (s2) l2"))).and(hasProperty("winRate", is(0.0))),
                both(hasProperty("playerFriendlyName", is("f5 (s5) l5"))).and(hasProperty("winRate", is(0.0)))
        ));
    }

    @Test
    void shouldReturnEachPlayerEfficiencies() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 5);

        assertThat(monthlyRating.getPlayerRatings(), containsInAnyOrder(
                both(hasProperty("playerFriendlyName", is("f6 (s6) l6"))).and(hasProperty("efficiency", is(0.7))),
                both(hasProperty("playerFriendlyName", is("f1 (s1) l1"))).and(hasProperty("efficiency", is(0.58))),
                both(hasProperty("playerFriendlyName", is("f4 (s4) l4"))).and(hasProperty("efficiency", is(0.45))),
                both(hasProperty("playerFriendlyName", is("f3 (s3) l3"))).and(hasProperty("efficiency", is(0.44))),
                both(hasProperty("playerFriendlyName", is("f2 (s2) l2"))).and(hasProperty("efficiency", is(0.37))),
                both(hasProperty("playerFriendlyName", is("f5 (s5) l5"))).and(hasProperty("efficiency", is(0.53)))
        ));
    }

    @Test
    void shouldReturnCorrectlyOrderedPlayers() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 1);

        assertThat(monthlyRating.getPlayerRatings(), contains(
                hasProperty("playerFriendlyName", is("f6 (s6) l6")), hasProperty("playerFriendlyName", is("f1 (s1) l1")),
                hasProperty("playerFriendlyName", is("f5 (s5) l5")), hasProperty("playerFriendlyName", is("f4 (s4) l4")),
                hasProperty("playerFriendlyName", is("f3 (s3) l3")), hasProperty("playerFriendlyName", is("f2 (s2) l2"))
        ));
    }

    @Test
    void shouldPutPlayersOverRatingThresholdOnTop() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 5);

        assertThat(monthlyRating.getPlayerRatings(), contains(
                hasProperty("playerFriendlyName", is("f1 (s1) l1")), hasProperty("playerFriendlyName", is("f3 (s3) l3")),
                hasProperty("playerFriendlyName", is("f6 (s6) l6")), hasProperty("playerFriendlyName", is("f5 (s5) l5")),
                hasProperty("playerFriendlyName", is("f4 (s4) l4")), hasProperty("playerFriendlyName", is("f2 (s2) l2"))
        ));
    }

    @Test
    void shouldSetZeroPlacesCountForMissingPlaces() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        Map<Integer, Long> player6Places = monthlyRating.getPlayerRatings().stream()
                .filter(playerMonthlyRating -> playerMonthlyRating.getPlayerFriendlyName().equals("f6 (s6) l6"))
                .findFirst().orElseThrow()
                .getOrderedPlaceCountByPlaceNames();

        assertEquals(1, player6Places.get(1));
        assertEquals(0, player6Places.get(2));
        assertEquals(1, player6Places.get(3));
        assertEquals(0, player6Places.get(4));
    }

    @Test
    void shouldExcludeZeroPlacesFromRating() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        Map<Integer, Long> player6Places = monthlyRating.getPlayerRatings().stream()
                .filter(playerMonthlyRating -> playerMonthlyRating.getPlayerFriendlyName().equals("f6 (s6) l6"))
                .findFirst().orElseThrow()
                .getOrderedPlaceCountByPlaceNames();

        assertEquals(4, player6Places.size());
    }

    @Test
    void shouldNotConsiderNoWinsPlayersInRating() {
        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        boolean isPlayer7Present = monthlyRating.getPlayerRatings().stream()
                .anyMatch(playerMonthlyRating -> playerMonthlyRating.getPlayerFriendlyName().equals("f7 (s7) l7"));

        assertFalse(isPlayer7Present);
    }

    @Test
    void shouldExcludeGuestPlayersFromRating() {
        matchPlayers.stream()
                .map(MatchPlayer::getPlayer)
                .filter(player -> Set.of("s5", "s6").contains(player.getSteamName()))
                .forEach(player -> player.setGuest(true));

        RatingReport monthlyRating = new RatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerRatings(), containsInAnyOrder(
                both(hasProperty("playerFriendlyName", is("f1 (s1) l1"))).and(hasProperty("matchesCount", is(6L))),
                both(hasProperty("playerFriendlyName", is("f4 (s4) l4"))).and(hasProperty("matchesCount", is(4L))),
                both(hasProperty("playerFriendlyName", is("f3 (s3) l3"))).and(hasProperty("matchesCount", is(5L))),
                both(hasProperty("playerFriendlyName", is("f2 (s2) l2"))).and(hasProperty("matchesCount", is(4L)))
        ));
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
