package ru.trainithard.dunebot.service.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.service.report.RatingReport.EntityRating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class LeadersRatingReportTest {
    private final List<MatchPlayer> matchPlayers = new ArrayList<>();
    private MatchPlayer matchPlayer6;
    private MatchPlayer matchPlayer7;
    private MatchPlayer matchPlayer5;

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
        matchPlayer6 = getMatchPlayer(match6, 6, 3);
        matchPlayers.add(matchPlayer6);
        matchPlayer7 = getMatchPlayer(match6, 7, 0);
        matchPlayers.add(matchPlayer7);
        matchPlayer5 = getMatchPlayer(match6, 5, 2);
        matchPlayers.add(matchPlayer5);
        matchPlayers.add(getMatchPlayer(match6, 3, 4));
        matchPlayers.add(getMatchPlayer(match6, 1, 3));
    }

    @Test
    void shouldCountAllLeaderMatches() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getMatchesCount()).isEqualTo(6);
    }

    @Test
    void shouldNotCountMatchesWithMissingLeader() {
        matchPlayer6.setLeader(null);

        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getMatchesCount()).isEqualTo(5);
    }

    @Test
    void shouldNotCountMatchesWithDuplicateLeader() {
        Leader leader = new Leader();
        leader.setId(5L);
        matchPlayer6.setLeader(leader);

        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getMatchesCount()).isEqualTo(5);
    }

    @Test
    void shouldCountMatchesWithMissingLeaderForNonParticipant() {
        matchPlayer7.setLeader(null);

        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getMatchesCount()).isEqualTo(6);
    }

    @Test
    void shouldReturnEachLeaderMatchesCounts() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName, EntityRating::getMatchesCount)
                .containsExactlyInAnyOrder(
                        tuple("LEADER 6", 2L),
                        tuple("LEADER 1", 6L),
                        tuple("LEADER 4", 4L),
                        tuple("LEADER 3", 5L),
                        tuple("LEADER 2", 4L),
                        tuple("LEADER 5", 3L)
                );
    }

    @Test
    void shouldReturnEachLeaderWinRates() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName, EntityRating::getWinRate)
                .containsExactlyInAnyOrder(
                        tuple("LEADER 6", 50.0),
                        tuple("LEADER 1", 33.33),
                        tuple("LEADER 4", 25.0),
                        tuple("LEADER 3", 20.0),
                        tuple("LEADER 2", 0.0),
                        tuple("LEADER 5", 0.0)
                );
    }

    @Test
    void shouldReturnEachLeaderEfficiencies() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 5);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName, EntityRating::getEfficiency)
                .containsExactlyInAnyOrder(
                        tuple("LEADER 6", 0.7),
                        tuple("LEADER 1", 0.58),
                        tuple("LEADER 4", 0.45),
                        tuple("LEADER 3", 0.44),
                        tuple("LEADER 2", 0.37),
                        tuple("LEADER 5", 0.53)
                );
    }

    @Test
    void shouldReturnCorrectlyOrderedLeaders() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 1);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName)
                .containsExactly("LEADER 6", "LEADER 1", "LEADER 5", "LEADER 4", "LEADER 3", "LEADER 2");
    }

    @Test
    void shouldPutLeadersOverRatingThresholdOnTop() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 5);

        assertThat(monthlyRating.getPlayerEntityRatings())
                .extracting(EntityRating::getName)
                .containsExactly("LEADER 1", "LEADER 3", "LEADER 6", "LEADER 5", "LEADER 4", "LEADER 2");
    }

    @Test
    void shouldSetZeroPlacesCountForMissingLeaders() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        Set<EntityRating> actualPlayerEntityRatings = monthlyRating.getPlayerEntityRatings();

        assertThat(actualPlayerEntityRatings)
                .filteredOn(playerEntityRating -> "LEADER 6".equals(playerEntityRating.getName()))
                .extracting(EntityRating::getOrderedPlaceCountByPlaceNames)
                .flatExtracting(Map::values)
                .contains(1L, 0L, 1L, 0L);
    }

    @Test
    void shouldExcludeZeroPlacesFromRating() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        Map<Integer, Long> player6Places = monthlyRating.getPlayerEntityRatings().stream()
                .filter(playerEntityRating -> playerEntityRating.getName().equals("LEADER 6"))
                .findFirst().orElseThrow()
                .getOrderedPlaceCountByPlaceNames();

        assertThat(player6Places).hasSize(4);
    }

    @Test
    void shouldNotConsiderNoWinsLeadersInRating() {
        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);

        boolean isPlayer7Present = monthlyRating.getPlayerEntityRatings().stream()
                .anyMatch(playerEntityRating -> playerEntityRating.getName().equals("f7 (s7) l7"));

        assertThat(isPlayer7Present).isFalse();
    }

    @Test
    void shouldNotConsiderLeadersWithoutPlayersPlace() {
        matchPlayers.stream()
                .filter(matchPlayer -> "s6".equals(matchPlayer.getPlayer().getSteamName()) && matchPlayer.getMatch().getId().equals(6L))
                .forEach(matchPlayer -> matchPlayer.setPlace(null));

        LeadersRatingReport monthlyRating = new LeadersRatingReport(matchPlayers, ModType.CLASSIC, 15);
        Set<EntityRating> actualPlayerEntityRatings = monthlyRating.getPlayerEntityRatings();

        assertThat(actualPlayerEntityRatings)
                .filteredOn(playersEntityRating -> playersEntityRating.getName().equals("LEADER 6"))
                .extracting(EntityRating::getMatchesCount, EntityRating::getWinRate, EntityRating::getEfficiency)
                .containsExactly(tuple(1L, 100.0, 1.0));
    }

    private MatchPlayer getMatchPlayer(Match match, long playerId, Integer place) {
        MatchPlayer matchPlayer = new MatchPlayer(match, getPlayer(playerId));
        matchPlayer.setPlace(place);
        Leader leader = new Leader();
        leader.setName("LEADER " + playerId);
        leader.setId(playerId);
        matchPlayer.setLeader(leader);
        return matchPlayer;
    }

    private Match getMatch(int matchId) {
        Match match = new Match();
        match.setId((long) matchId);
        match.setModType(ModType.CLASSIC);
        return match;
    }

    private Player getPlayer(long playerId) {
        Player player = new Player();
        player.setId(playerId);
        player.setFirstName("f" + playerId);
        player.setSteamName("s" + playerId);
        player.setLastName("l" + playerId);
        return player;
    }
}
