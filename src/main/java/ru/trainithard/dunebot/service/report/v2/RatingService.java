package ru.trainithard.dunebot.service.report.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {
    private final Clock clock;
    private final MatchRepository matchRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final LeaderRatingRepository leaderRatingRepository;
    private final RatingUpdateService<PlayerRating> playerRatingUpdateService;
    private final RatingUpdateService<LeaderRating> leaderRatingUpdateService;
    private final MetaDataService metaDataService;

    public void buildFullRating() {
        log.debug("Full rating calculation...");
        LocalDate playersRatingDate = metaDataService.findRatingDate(MetaDataKey.PLAYER_RATING_DATE).plusDays(1);
        LocalDate leadersRatingDate = metaDataService.findRatingDate(MetaDataKey.LEADER_RATING_DATE).plusDays(1);

        List<PlayerRating> latestPlayerRatings = playerRatingRepository.findLatestPlayerRatings();
        List<LeaderRating> latestLeaderRatings = leaderRatingRepository.findLatestLeaderRatings();
        log.debug("Found {} player_ratings, {} leader ratings", latestPlayerRatings.size(), latestLeaderRatings.size());

        LocalDate today = LocalDate.now(clock);
        LocalDate selectionStartDate = playersRatingDate.isBefore(leadersRatingDate) ? playersRatingDate : leadersRatingDate;
        List<Match> matches = matchRepository.findAllByDatesAndState(selectionStartDate, today, List.of(MatchState.FINISHED));
        log.debug("Found {} matches", matches.size());
        List<Match> playerRatingMatches = new ArrayList<>();
        List<Match> leaderRatingMatches = new ArrayList<>();
        matches.forEach(match -> {
            if (match.getFinishDate().isAfter(playersRatingDate)) {
                playerRatingMatches.add(match);
            }
            if (match.getFinishDate().isAfter(leadersRatingDate)) {
                leaderRatingMatches.add(match);
            }
        });

        playerRatingUpdateService.updateRatings(playerRatingMatches, latestPlayerRatings);
        leaderRatingUpdateService.updateRatings(leaderRatingMatches, latestLeaderRatings);
        log.debug("Full rating calculation finished");
    }
}
