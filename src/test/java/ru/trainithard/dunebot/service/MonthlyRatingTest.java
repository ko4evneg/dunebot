package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MonthlyRatingTest {
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
    }

    @Test
    void shouldCountAllPlayerMatches() {
        MonthlyRating monthlyRating = new MonthlyRating(matchPlayers, ModType.CLASSIC);

        assertEquals(6, monthlyRating.getMatchesCount());
    }

    @Test
    void shouldReturnEachPlayerMatchesCounts() {
        MonthlyRating monthlyRating = new MonthlyRating(matchPlayers, ModType.CLASSIC);

        assertThat(monthlyRating.getPlayerRatings(), containsInAnyOrder(
                both(hasProperty("playerFriendlyName", is("s6 (f6)"))).and(hasProperty("matchesCount", is(2L))),
                both(hasProperty("playerFriendlyName", is("s1 (f1)"))).and(hasProperty("matchesCount", is(6L))),
                both(hasProperty("playerFriendlyName", is("s4 (f4)"))).and(hasProperty("matchesCount", is(4L))),
                both(hasProperty("playerFriendlyName", is("s3 (f3)"))).and(hasProperty("matchesCount", is(5L))),
                both(hasProperty("playerFriendlyName", is("s2 (f2)"))).and(hasProperty("matchesCount", is(4L))),
                both(hasProperty("playerFriendlyName", is("s5 (f5)"))).and(hasProperty("matchesCount", is(3L)))
        ));
    }

    @Test
    void shouldReturnEachPlayerWinRates() {
        MonthlyRating monthlyRating = new MonthlyRating(matchPlayers, ModType.CLASSIC);

        assertThat(monthlyRating.getPlayerRatings(), containsInAnyOrder(
                both(hasProperty("playerFriendlyName", is("s6 (f6)"))).and(hasProperty("winRate", is(50.0))),
                both(hasProperty("playerFriendlyName", is("s1 (f1)"))).and(hasProperty("winRate", is(33.33))),
                both(hasProperty("playerFriendlyName", is("s4 (f4)"))).and(hasProperty("winRate", is(25.0))),
                both(hasProperty("playerFriendlyName", is("s3 (f3)"))).and(hasProperty("winRate", is(20.0))),
                both(hasProperty("playerFriendlyName", is("s2 (f2)"))).and(hasProperty("winRate", is(0.0))),
                both(hasProperty("playerFriendlyName", is("s5 (f5)"))).and(hasProperty("winRate", is(0.0)))
        ));
    }

    @Test
    void shouldReturnEachPlayerEfficiencies() {
        MonthlyRating monthlyRating = new MonthlyRating(matchPlayers, ModType.CLASSIC);

        assertThat(monthlyRating.getPlayerRatings(), containsInAnyOrder(
                both(hasProperty("playerFriendlyName", is("s6 (f6)"))).and(hasProperty("efficiency", is(0.7))),
                both(hasProperty("playerFriendlyName", is("s1 (f1)"))).and(hasProperty("efficiency", is(0.58))),
                both(hasProperty("playerFriendlyName", is("s4 (f4)"))).and(hasProperty("efficiency", is(0.45))),
                both(hasProperty("playerFriendlyName", is("s3 (f3)"))).and(hasProperty("efficiency", is(0.44))),
                both(hasProperty("playerFriendlyName", is("s2 (f2)"))).and(hasProperty("efficiency", is(0.37))),
                both(hasProperty("playerFriendlyName", is("s5 (f5)"))).and(hasProperty("efficiency", is(0.53)))
        ));
    }

    @Test
    void shouldReturnCorrectlyOrderedPlayers() {
        MonthlyRating monthlyRating = new MonthlyRating(matchPlayers, ModType.CLASSIC);

        assertThat(monthlyRating.getPlayerRatings(), contains(
                hasProperty("playerFriendlyName", is("s6 (f6)")), hasProperty("playerFriendlyName", is("s1 (f1)")),
                hasProperty("playerFriendlyName", is("s5 (f5)")), hasProperty("playerFriendlyName", is("s4 (f4)")),
                hasProperty("playerFriendlyName", is("s3 (f3)")), hasProperty("playerFriendlyName", is("s2 (f2)"))
        ));
    }

    @Test
    void shouldSetZeroPlacesCountForMissingPlaces() {
        MonthlyRating monthlyRating = new MonthlyRating(matchPlayers, ModType.CLASSIC);

        Map<Integer, Long> player6Places = monthlyRating.getPlayerRatings().stream()
                .filter(playerMonthlyRating -> playerMonthlyRating.getPlayerFriendlyName().equals("s6 (f6)"))
                .findFirst().orElseThrow()
                .getOrderedPlaceCountByPlaceNames();

        assertEquals(1, player6Places.get(1));
        assertEquals(0, player6Places.get(2));
        assertEquals(1, player6Places.get(3));
        assertEquals(0, player6Places.get(4));
    }

    private MatchPlayer getMatchPlayer(Match match, int playerId, int place) {
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
        return player;
    }
}
