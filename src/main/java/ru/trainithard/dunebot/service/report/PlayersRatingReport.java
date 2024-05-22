package ru.trainithard.dunebot.service.report;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Rateable;

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
    Map<Rateable, List<MatchPlayer>> getRateableMatchPlayers(List<MatchPlayer> monthMatchPlayers) {
        return monthMatchPlayers.stream()
                .filter(matchPlayer -> !matchPlayer.getPlayer().isGuest() && matchPlayer.hasRateablePlace())
                .collect(Collectors.groupingBy(MatchPlayer::getPlayer));
    }

    @Override
    TreeMap<Integer, Long> getOrderedPlaceCountByPlaceNames(List<MatchPlayer> matchPlayers) {
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
