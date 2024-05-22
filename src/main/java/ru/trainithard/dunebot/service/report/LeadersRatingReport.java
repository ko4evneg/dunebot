package ru.trainithard.dunebot.service.report;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Rateable;

import java.util.*;
import java.util.stream.Collectors;

@Getter
class LeadersRatingReport extends RatingReport {
    private Map<Match, List<MatchPlayer>> matchPlayersByLeaderRateableMatch;

    LeadersRatingReport(@NotEmpty List<MatchPlayer> monthMatchPlayers, ModType modType, int matchesRatingThreshold) {
        super(monthMatchPlayers, modType, matchesRatingThreshold);
    }

    @Override
    int calculateMatchesCount(List<MatchPlayer> monthMatchPlayers) {
        this.matchPlayersByLeaderRateableMatch = monthMatchPlayers.stream()
                .filter(MatchPlayer::hasRateablePlace)
                .collect(Collectors.groupingBy(MatchPlayer::getMatch))
                .entrySet().stream()
                .filter(entry -> isLeaderRateableMatch(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return matchPlayersByLeaderRateableMatch.size();
    }

    private boolean isLeaderRateableMatch(List<MatchPlayer> matchPlayers) {
        if (matchPlayers.isEmpty()) {
            return false;
        }
        long uniqueLeadersCount = matchPlayers.stream()
                .map(MatchPlayer::getLeader)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        return uniqueLeadersCount == matchPlayersCount;
    }

    @Override
    Map<Rateable, List<MatchPlayer>> getRateableMatchPlayers(List<MatchPlayer> monthMatchPlayers) {
        return matchPlayersByLeaderRateableMatch.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(MatchPlayer::getLeader));
    }

    @Override
    TreeMap<Integer, Long> getOrderedPlaceCountByPlaceNames(List<MatchPlayer> matchPlayers) {
        TreeMap<Integer, Long> result = new TreeMap<>();
        for (MatchPlayer matchPlayer : matchPlayers) {
            Integer playerPlace = matchPlayer.getPlace();
            result.merge(playerPlace, 1L, Long::sum);
        }
        for (int i = 1; i <= matchPlayersCount; i++) {
            result.putIfAbsent(i, 0L);
        }
        return result;
    }
}
