package ru.trainithard.dunebot.service.report;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Rateable;

import java.util.*;

@Getter
abstract class RatingReport {
    final int matchPlayersCount;
    final int matchesCount;
    final int matchesRatingThreshold;
    final Set<EntityRating> playerEntityRatings = new TreeSet<>();

    RatingReport(@NotEmpty List<MatchPlayer> monthMatchPlayers, ModType modType, int matchesRatingThreshold) {
        this.matchesRatingThreshold = matchesRatingThreshold;
        this.matchPlayersCount = modType.getPlayersCount();
        this.matchesCount = calculateMatchesCount(monthMatchPlayers);
        fillEntityRatings(monthMatchPlayers);
    }

    abstract int calculateMatchesCount(List<MatchPlayer> monthMatchPlayers);

    void fillEntityRatings(List<MatchPlayer> monthMatchPlayers) {
        getRateableMatchPlayers(monthMatchPlayers).entrySet().stream()
                .forEach(entry -> {
                    Map<Integer, Long> orderedPlaceCountByPlaceNames =
                            getOrderedPlaceCountByPlaceNames(entry.getValue());
                    long firstPlacesCount = orderedPlaceCountByPlaceNames.getOrDefault(1, 0L);
                    long leaderMatchesCount = orderedPlaceCountByPlaceNames.values().stream().mapToLong(Long::longValue).sum();
                    double winRate = RatingCalculator.calculateWinRate(firstPlacesCount, leaderMatchesCount);
                    double efficiency = RatingCalculator.calculateEfficiency(orderedPlaceCountByPlaceNames, leaderMatchesCount);

                    String friendlyName = entry.getKey().getRatingName();
                    EntityRating entityRating =
                            new EntityRating(friendlyName, orderedPlaceCountByPlaceNames, leaderMatchesCount, efficiency, winRate);
                    playerEntityRatings.add(entityRating);
                });
    }

    abstract Map<Rateable, List<MatchPlayer>> getRateableMatchPlayers(List<MatchPlayer> monthMatchPlayers);

    abstract TreeMap<Integer, Long> getOrderedPlaceCountByPlaceNames(List<MatchPlayer> matchPlayers);

    @Getter
    @RequiredArgsConstructor
    public class EntityRating implements Comparable<EntityRating> {
        private final String name;
        private final Map<Integer, Long> orderedPlaceCountByPlaceNames;
        private final long matchesCount;
        private final double efficiency;
        private final double winRate;

        @Override
        public int compareTo(@NotNull RatingReport.EntityRating comparedEntityRating) {
            if (this.matchesCount >= matchesRatingThreshold && comparedEntityRating.matchesCount < matchesRatingThreshold) {
                return -1;
            }
            if (comparedEntityRating.matchesCount >= matchesRatingThreshold && this.matchesCount < matchesRatingThreshold) {
                return 1;
            }
            int reversedEfficiencyDiff = (int) (comparedEntityRating.efficiency * 100 - this.efficiency * 100);
            return reversedEfficiencyDiff != 0
                    ? reversedEfficiencyDiff
                    : this.name.compareTo(comparedEntityRating.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EntityRating that = (EntityRating) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
