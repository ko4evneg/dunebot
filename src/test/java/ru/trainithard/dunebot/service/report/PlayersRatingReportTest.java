package ru.trainithard.dunebot.service.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.report.RatingReport.EntityRating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class PlayersRatingReportTest {
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
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getMatchesCount()).isEqualTo(6);
    }

    @Test
    void shouldNotThrowOnNullPlace() {
        matchPlayers.add(getMatchPlayer(getMatch(7), 7, null));

        assertThatCode(() -> new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldReturnEachPlayerMatchesCounts() {
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName, EntityRating::getMatchesCount)
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
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName, EntityRating::getWinRate)
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
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 5);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName, EntityRating::getEfficiency)
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
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 1);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName)
                .containsExactly("f6 (s6) l6", "f1 (s1) l1", "f5 (s5) l5", "f4 (s4) l4", "f3 (s3) l3", "f2 (s2) l2");
    }

    @Test
    void shouldPutPlayersOverRatingThresholdOnTop() {
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 5);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName)
                .containsExactly("f1 (s1) l1", "f3 (s3) l3", "f6 (s6) l6", "f5 (s5) l5", "f4 (s4) l4", "f2 (s2) l2");
    }

    @Test
    void shouldSetZeroPlacesCountForMissingPlaces() {
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        Set<EntityRating> actualPlayerEntityRatings = monthlyRating.getPlayerEntityRatings();

        assertThat(actualPlayerEntityRatings)
                .filteredOn(playerEntityRating -> "f6 (s6) l6".equals(playerEntityRating.getName()))
                .extracting(EntityRating::getOrderedPlaceCountByPlaceNames)
                .flatExtracting(Map::values)
                .contains(1L, 0L, 1L, 0L);
    }

    @Test
    void shouldExcludeZeroPlacesFromRating() {
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        Map<Integer, Long> player6Places = monthlyRating.getPlayerEntityRatings().stream()
                .filter(playerEntityRating -> playerEntityRating.getName().equals("f6 (s6) l6"))
                .findFirst().orElseThrow()
                .getOrderedPlaceCountByPlaceNames();

        assertThat(player6Places).hasSize(4);
    }

    @Test
    void shouldNotConsiderNoWinsPlayersInRating() {
        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        boolean isPlayer7Present = monthlyRating.getPlayerEntityRatings().stream()
                .anyMatch(playerEntityRating -> playerEntityRating.getName().equals("f7 (s7) l7"));

        assertThat(isPlayer7Present).isFalse();
    }

    @Test
    void shouldExcludeGuestPlayersFromRating() {
        matchPlayers.stream()
                .map(MatchPlayer::getPlayer)
                .filter(player -> Set.of("s5", "s6").contains(player.getSteamName()))
                .forEach(player -> player.setGuest(true));

        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName, EntityRating::getMatchesCount)
                .containsExactlyInAnyOrder(
                        tuple("f1 (s1) l1", 6L),
                        tuple("f4 (s4) l4", 4L),
                        tuple("f3 (s3) l3", 5L),
                        tuple("f2 (s2) l2", 4L)
                );
    }

    @Test
    void shouldNotConsiderPlayersWithoutPlace() {
        matchPlayers.stream()
                .filter(matchPlayer -> "s6".equals(matchPlayer.getPlayer().getSteamName()) && matchPlayer.getMatch().getId().equals(6L))
                .forEach(matchPlayer -> matchPlayer.setPlace(null));

        PlayersRatingReport monthlyRating = new PlayersRatingReport(matchPlayers, ModType.CLASSIC, 15);
        Set<EntityRating> actualPlayerEntityRatings = monthlyRating.getPlayerEntityRatings();

        assertThat(actualPlayerEntityRatings)
                .filteredOn(playersEntityRating -> playersEntityRating.getName().equals("f6 (s6) l6"))
                .extracting(EntityRating::getMatchesCount, EntityRating::getWinRate, EntityRating::getEfficiency)
                .containsExactly(tuple(1L, 100.0, 1.0));
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
