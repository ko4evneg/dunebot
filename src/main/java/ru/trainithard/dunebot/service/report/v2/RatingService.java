package ru.trainithard.dunebot.service.report.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.repository.LeaderRatingRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRatingRepository;
import ru.trainithard.dunebot.service.MetaDataService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {
    private static final LocalDate LEADERS_STAT_START_DATE = LocalDate.of(2024, 7, 15);
    private final Clock clock;
    private final MatchRepository matchRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final LeaderRatingRepository leaderRatingRepository;
    private final RatingUpdateService<PlayerRating> playerRatingUpdateService;
    private final RatingUpdateService<LeaderRating> leaderRatingUpdateService;
    private final MetaDataService metaDataService;
    private final CacheManager cacheManager;

    public void buildFullRating() {
        log.debug("Full rating calculation...");
        LocalDate playersRatingDate = metaDataService.findRatingDate(MetaDataKey.PLAYER_RATING_DATE);
        LocalDate leadersRatingDate = metaDataService.findRatingDate(MetaDataKey.LEADER_RATING_DATE);

        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDate selectionStartDate = playersRatingDate.isBefore(leadersRatingDate) ? playersRatingDate : leadersRatingDate;
        selectionStartDate = selectionStartDate.plusDays(1);
        List<Match> matches = matchRepository.findAllByDatesAndState(selectionStartDate, yesterday, List.of(MatchState.FINISHED));
        log.debug("Found {} matches", matches.size());
        if (matches.isEmpty()) {
            return;
        }

        List<Match> playerRatingMatches = new ArrayList<>();
        List<Match> leaderRatingMatches = new ArrayList<>();
        matches.forEach(match -> {
            if (match.getFinishDate().isAfter(playersRatingDate)) {
                playerRatingMatches.add(match);
            }
            if (match.getFinishDate().isAfter(leadersRatingDate) && match.getFinishDate().isAfter(LEADERS_STAT_START_DATE)) {
                leaderRatingMatches.add(match);
            }
        });

        List<PlayerRating> latestPlayerRatings = playerRatingRepository.findLatestPlayerRatings();
        List<LeaderRating> latestLeaderRatings = leaderRatingRepository.findLatestLeaderRatings();
        log.debug("Found {} player_ratings, {} leader ratings", latestPlayerRatings.size(), latestLeaderRatings.size());

        playerRatingUpdateService.updateRatings(playerRatingMatches, latestPlayerRatings, yesterday);
        leaderRatingUpdateService.updateRatings(leaderRatingMatches, latestLeaderRatings, yesterday);
        log.debug("Full rating calculation finished");

        clearCache();
    }

    private void clearCache() {
        Cache playerRatingsCache = cacheManager.getCache("playerRatings");
        if (!Objects.isNull(playerRatingsCache)) {
            playerRatingsCache.clear();
        }
    }
}
