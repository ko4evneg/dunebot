package ru.trainithard.dunebot.service;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class MonthlyRating {
    private final int matchesCount;
    private final Set<PlayerMonthlyRating> playerRatings = new TreeSet<>();
    @Getter(AccessLevel.NONE)
    private final Map<Integer, Double> efficiencyRateByPlaceNames = new HashMap<>();

    public MonthlyRating(@NotEmpty List<MatchPlayer> monthMatchPlayers, ModType modType) {
        this.matchesCount = getMatchesCount(monthMatchPlayers);
        fillPlayerRatings(monthMatchPlayers, modType.getPlayersCount());
    }

    private int getMatchesCount(List<MatchPlayer> monthMatchPlayers) {
        return (int) monthMatchPlayers.stream()
                .filter(matchPlayer -> Objects.requireNonNull(matchPlayer.getPlace()) != 0)
                .map(matchPlayer -> matchPlayer.getMatch().getId())
                .distinct()
                .count();
    }

    private void fillPlayerRatings(List<MatchPlayer> monthMatchPlayers, int matchPlayersCount) {
        fillDefaultEfficiencies();

        Map<Player, List<MatchPlayer>> matchPlayersByPlayer = monthMatchPlayers.stream()
                .collect(Collectors.groupingBy(MatchPlayer::getPlayer));

        matchPlayersByPlayer.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(matchPlayer -> Objects.requireNonNull(matchPlayer.getPlace()) != 0))
                .forEach(entry -> {
                    Map<Integer, Long> orderedPlaceCountByPlaceNames = getOrderedPlaceCountByPlaceNames(entry.getValue(), matchPlayersCount);
                    long firstPlacesCount = orderedPlaceCountByPlaceNames.getOrDefault(1, 0L);
                    long playerMatchesCount = orderedPlaceCountByPlaceNames.values().stream().mapToLong(Long::longValue).sum();
                    double winRate = calculateWinRate(firstPlacesCount, playerMatchesCount);
                    double efficiency = calculateEfficiency(orderedPlaceCountByPlaceNames, playerMatchesCount);

                    PlayerMonthlyRating playerMonthlyRating =
                            new PlayerMonthlyRating(entry.getKey().getFriendlyName(), orderedPlaceCountByPlaceNames, playerMatchesCount, efficiency, winRate);
                    playerRatings.add(playerMonthlyRating);
                });
    }

    private void fillDefaultEfficiencies() {
        efficiencyRateByPlaceNames.put(1, 1.0);
        efficiencyRateByPlaceNames.put(2, 0.6);
        efficiencyRateByPlaceNames.put(3, 0.4);
        efficiencyRateByPlaceNames.put(4, 0.1);
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
        private final String playerFriendlyName;
        private final Map<Integer, Long> orderedPlaceCountByPlaceNames;
        private final long matchesCount;
        private final double efficiency;
        private final double winRate;

        @Override
        public int compareTo(PlayerMonthlyRating comparedRating) {
            int reversedEfficiencyDiff = (int) -(this.efficiency * 100 - comparedRating.efficiency * 100);
            return reversedEfficiencyDiff != 0 ? reversedEfficiencyDiff : this.playerFriendlyName.compareTo(comparedRating.playerFriendlyName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlayerMonthlyRating that = (PlayerMonthlyRating) o;
            return Objects.equals(playerFriendlyName, that.playerFriendlyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerFriendlyName);
        }
    }
}