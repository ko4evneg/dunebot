package ru.trainithard.dunebot.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class MonthlyRating {
    private final String reportName;
    private final int matchesCount;
    private final Set<PlayerMonthlyRating> playerRatings = new TreeSet<>();
    @Getter(AccessLevel.NONE)
    private final Map<Integer, Double> efficiencyRateByPlaceNames = new HashMap<>();

    public MonthlyRating(String reportName, List<MatchPlayer> monthMatchPlayers) {
        this.reportName = reportName;
        this.matchesCount = getMatchesCount(monthMatchPlayers);
        fillPlayerRatings(monthMatchPlayers);
    }

    private int getMatchesCount(List<MatchPlayer> monthMatchPlayers) {
        return (int) monthMatchPlayers.stream()
                .map(matchPlayer -> matchPlayer.getMatch().getId())
                .distinct()
                .count();
    }

    private void fillPlayerRatings(List<MatchPlayer> monthMatchPlayers) {
        fillDefaultEfficiencies();

        Map<Player, List<MatchPlayer>> matchPlayersByPlayer = monthMatchPlayers.stream()
                .collect(Collectors.groupingBy(MatchPlayer::getPlayer));

        matchPlayersByPlayer.forEach((player, matchPlayers) -> {
            Map<Integer, Long> placeCountByPlaceNames = matchPlayers.stream()
                    .collect(Collectors.groupingBy(Objects.requireNonNull(MatchPlayer::getPlace), Collectors.counting()));
            long firstPlacesCount = placeCountByPlaceNames.getOrDefault(1, 0L);
            long playerMatchesCount = placeCountByPlaceNames.values().stream().mapToLong(Long::longValue).sum();
            double winRate = calculateWinRate(firstPlacesCount, playerMatchesCount);
            double efficiency = calculateEfficiency(placeCountByPlaceNames, playerMatchesCount);

            PlayerMonthlyRating playerMonthlyRating =
                    new PlayerMonthlyRating(player.getFriendlyName(), placeCountByPlaceNames, playerMatchesCount, efficiency, winRate);
            playerRatings.add(playerMonthlyRating);
        });
    }

    private void fillDefaultEfficiencies() {
        efficiencyRateByPlaceNames.put(1, 1.0);
        efficiencyRateByPlaceNames.put(2, 0.6);
        efficiencyRateByPlaceNames.put(3, 0.4);
        efficiencyRateByPlaceNames.put(4, 0.1);
    }

    private double calculateWinRate(double firstPlacesCount, long matchesCount) {
        double winRate = firstPlacesCount / matchesCount * 100;
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
    public static class PlayerMonthlyRating implements Comparable<PlayerMonthlyRating> {
        private final String playerName;
        private final Map<Integer, Long> placeCountByPlaceNames;
        private final long matchesCount;
        private final double efficiency;
        private final double winRate;

        @Override
        public int compareTo(PlayerMonthlyRating comparedRating) {
            int reversedEfficiencyDiff = (int) -(this.efficiency * 100 - comparedRating.efficiency * 100);
            return reversedEfficiencyDiff != 0 ? reversedEfficiencyDiff : this.playerName.compareTo(comparedRating.playerName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlayerMonthlyRating that = (PlayerMonthlyRating) o;
            return Objects.equals(playerName, that.playerName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerName);
        }
    }
}
