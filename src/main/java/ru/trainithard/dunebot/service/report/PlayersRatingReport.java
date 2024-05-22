package ru.trainithard.dunebot.service.report;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Getter
class PlayersRatingReport extends RatingReport {
    PlayersRatingReport(@NotEmpty List<MatchPlayer> monthMatchPlayers, ModType modType, int matchesRatingThreshold) {
        super(monthMatchPlayers, modType, matchesRatingThreshold);
    }

    @Override
    int calculateMatchesCount(List<MatchPlayer> monthMatchPlayers) {
        return (int) monthMatchPlayers.stream()
                .filter(MatchPlayer::hasRateablePlace)
                .map(matchPlayer -> matchPlayer.getMatch().getId())
                .distinct()
                .count();
    }

    @Override
    void fillEntityRatings(List<MatchPlayer> monthMatchPlayers, int matchPlayersCount) {
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
                    EntityRating entityRating =
                            new EntityRating(friendlyName, orderedPlaceCountByPlaceNames, playerMatchesCount, efficiency, winRate);
                    playerEntityRatings.add(entityRating);
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
}
