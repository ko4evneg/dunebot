package ru.trainithard.dunebot.service.report;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;

import java.util.*;
import java.util.stream.Collectors;

@Getter
class RatingReport {
    private final int matchesCount;
    private final Set<PlayerMonthlyRating> playerRatings = new TreeSet<>();
    private final int matchesRatingThreshold;

    RatingReport(@NotEmpty List<MatchPlayer> monthMatchPlayers, ModType modType, int matchesRatingThreshold) {
        this.matchesCount = getMatchesCount(monthMatchPlayers);
        this.matchesRatingThreshold = matchesRatingThreshold;
        fillPlayerRatings(monthMatchPlayers, modType.getPlayersCount());
    }

    private int getMatchesCount(List<MatchPlayer> monthMatchPlayers) {
        return (int) monthMatchPlayers.stream()
                .filter(MatchPlayer::hasRateablePlace)
                .map(matchPlayer -> matchPlayer.getMatch().getId())
                .distinct()
                .count();
    }

    private void fillPlayerRatings(List<MatchPlayer> monthMatchPlayers, int matchPlayersCount) {
        Map<Player, List<MatchPlayer>> matchPlayersByPlayer = monthMatchPlayers.stream()
                .filter(matchPlayer -> !matchPlayer.getPlayer().isGuest())
                .collect(Collectors.groupingBy(MatchPlayer::getPlayer));

        matchPlayersByPlayer.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(MatchPlayer::hasRateablePlace))
                .forEach(entry -> {
                    Map<Integer, Long> orderedPlaceCountByPlaceNames =
                            getOrderedPlaceCountByPlaceNames(entry.getValue(), matchPlayersCount);
                    long firstPlacesCount = orderedPlaceCountByPlaceNames.getOrDefault(1, 0L);
                    long playerMatchesCount = orderedPlaceCountByPlaceNames.values().stream().mapToLong(Long::longValue).sum();
                    double winRate = RatingCalculator.calculateWinRate(firstPlacesCount, playerMatchesCount);
                    double efficiency = RatingCalculator.calculateEfficiency(orderedPlaceCountByPlaceNames, playerMatchesCount);

                    String friendlyName = entry.getKey().getFriendlyName();
                    PlayerMonthlyRating playerMonthlyRating =
                            new PlayerMonthlyRating(friendlyName, orderedPlaceCountByPlaceNames, playerMatchesCount, efficiency, winRate);
                    playerRatings.add(playerMonthlyRating);
                });
    }

    private TreeMap<Integer, Long> getOrderedPlaceCountByPlaceNames(List<MatchPlayer> matchPlayers, int matchPlayersCount) {
        TreeMap<Integer, Long> result = new TreeMap<>();
        for (MatchPlayer matchPlayer : matchPlayers) {
            if (matchPlayer.hasRateablePlace()) {
                result.merge(matchPlayer.getPlace(), 1L, Long::sum);
            }
        }
        for (int i = 1; i <= matchPlayersCount; i++) {
            result.putIfAbsent(i, 0L);
        }
        return result;
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
