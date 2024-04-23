package ru.trainithard.dunebot.service.report;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;

import java.util.*;
import java.util.stream.Collectors;

@Getter
class RatingReport {
    @Getter(AccessLevel.NONE)
    private static final Map<Integer, Double> efficiencyRateByPlaceNames = new HashMap<>();
    private final int matchesCount;
    private final Set<PlayerMonthlyRating> playerRatings = new TreeSet<>();
    private final int matchesRatingThreshold;

    static {
        efficiencyRateByPlaceNames.put(1, 1.0);
        efficiencyRateByPlaceNames.put(2, 0.6);
        efficiencyRateByPlaceNames.put(3, 0.4);
        efficiencyRateByPlaceNames.put(4, 0.1);
    }

    RatingReport(@NotEmpty List<MatchPlayer> monthMatchPlayers, ModType modType, int matchesRatingThreshold) {
        this.matchesCount = getMatchesCount(monthMatchPlayers);
        this.matchesRatingThreshold = matchesRatingThreshold;
        fillPlayerRatings(monthMatchPlayers, modType.getPlayersCount());
    }

    private int getMatchesCount(List<MatchPlayer> monthMatchPlayers) {
        return (int) monthMatchPlayers.stream()
                .filter(this::hasAssignedPlace)
                .map(matchPlayer -> matchPlayer.getMatch().getId())
                .distinct()
                .count();
    }

    private void fillPlayerRatings(List<MatchPlayer> monthMatchPlayers, int matchPlayersCount) {
        Map<Player, List<MatchPlayer>> matchPlayersByPlayer = monthMatchPlayers.stream()
                .filter(matchPlayer -> !matchPlayer.getPlayer().isGuest())
                .collect(Collectors.groupingBy(MatchPlayer::getPlayer));

        matchPlayersByPlayer.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(this::hasAssignedPlace))
                .forEach(entry -> {
                    Map<Integer, Long> orderedPlaceCountByPlaceNames =
                            getOrderedPlaceCountByPlaceNames(entry.getValue(), matchPlayersCount);
                    long firstPlacesCount = orderedPlaceCountByPlaceNames.getOrDefault(1, 0L);
                    long playerMatchesCount = orderedPlaceCountByPlaceNames.values().stream().mapToLong(Long::longValue).sum();
                    double winRate = calculateWinRate(firstPlacesCount, playerMatchesCount);
                    double efficiency = calculateEfficiency(orderedPlaceCountByPlaceNames, playerMatchesCount);

                    String friendlyName = entry.getKey().getFriendlyName();
                    PlayerMonthlyRating playerMonthlyRating =
                            new PlayerMonthlyRating(friendlyName, orderedPlaceCountByPlaceNames, playerMatchesCount, efficiency, winRate);
                    playerRatings.add(playerMonthlyRating);
                });
    }

    private boolean hasAssignedPlace(MatchPlayer matchPlayer) {
        return matchPlayer.getPlace() != null && matchPlayer.getPlace() != 0;
    }

    private TreeMap<Integer, Long> getOrderedPlaceCountByPlaceNames(List<MatchPlayer> matchPlayers, int matchPlayersCount) {
        TreeMap<Integer, Long> result = new TreeMap<>();
        for (MatchPlayer matchPlayer : matchPlayers) {
            if (Objects.requireNonNull(matchPlayer.getPlace()) != 0) {
                result.merge(matchPlayer.getPlace(), 1L, Long::sum);
            }
        }
        for (int i = 1; i <= matchPlayersCount; i++) {
            result.putIfAbsent(i, 0L);
        }
        return result;
    }

    private double calculateWinRate(double firstPlacesCount, long playerMatchesCount) {
        double winRate = firstPlacesCount / playerMatchesCount * 100;
        return (int) (winRate * 100) / 100.0;
    }

    private double calculateEfficiency(Map<Integer, Long> placeCountByPlaceNames, long allPlacesCount) {
        double efficiencesSum = 0;
        for (Map.Entry<Integer, Long> entry : placeCountByPlaceNames.entrySet()) {
            int place = entry.getKey();
            double placeEfficiency = efficiencyRateByPlaceNames.getOrDefault(place, 1.0);
            double placesCount = placeCountByPlaceNames.getOrDefault(place, 0L);
            efficiencesSum += placesCount / allPlacesCount * placeEfficiency;
        }
        return ((int) (efficiencesSum * 100)) / 100.0;

    }

    @Getter
    @RequiredArgsConstructor
    public class PlayerMonthlyRating implements Comparable<PlayerMonthlyRating> {
        private final String playerFriendlyName;
        private final Map<Integer, Long> orderedPlaceCountByPlaceNames;
        private final long matchesCount;
        private final double efficiency;
        private final double winRate;

        @Override
        public int compareTo(@NotNull PlayerMonthlyRating comparedRating) {
            if (this.matchesCount >= matchesRatingThreshold && comparedRating.matchesCount < matchesRatingThreshold) {
                return -1;
            }
            if (comparedRating.matchesCount >= matchesRatingThreshold && this.matchesCount < matchesRatingThreshold) {
                return 1;
            }
            int reversedEfficiencyDiff = (int) (comparedRating.efficiency * 100 - this.efficiency * 100);
            return reversedEfficiencyDiff != 0
                    ? reversedEfficiencyDiff
                    : this.playerFriendlyName.compareTo(comparedRating.playerFriendlyName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PlayerMonthlyRating that = (PlayerMonthlyRating) o;
            return Objects.equals(playerFriendlyName, that.playerFriendlyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerFriendlyName);
        }
    }
}
