package ru.trainithard.dunebot.service.report.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.repository.LeaderRatingRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRatingRepository;
import ru.trainithard.dunebot.service.LogId;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {
    private final Clock clock;
    private final MatchRepository matchRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final LeaderRatingRepository leaderRatingRepository;
    private final RatingMergeService<PlayerRating> playerRatingMergeService;
    private final RatingMergeService<LeaderRating> leaderRatingMergeService;

    public void buildFullRating() {
        log.debug("Full rating calculation...");

        List<PlayerRating> latestPlayerRatings = playerRatingRepository.findLatestPlayerRatings();
        List<LeaderRating> latestLeaderRatings = leaderRatingRepository.findLatestLeaderRatings();
        log.debug("Found {} player_ratings, {} leader ratings", latestPlayerRatings.size(), latestLeaderRatings.size());

        LocalDate today = LocalDate.now(clock);
        LocalDate selectionStartDate = getSelectionStartDate(latestPlayerRatings, latestLeaderRatings);
        List<Match> matches = matchRepository.findAllByDatesAndState(selectionStartDate, today, List.of(MatchState.FINISHED));
        log.debug("Found {} match", matches.size());

        playerRatingMergeService.updateRatings(matches, latestPlayerRatings);
        leaderRatingMergeService.updateRatings(matches, latestLeaderRatings);
        log.debug("Full rating saved...");
    }

    private LocalDate getSelectionStartDate(List<? extends AbstractRating> playerRatings, List<? extends AbstractRating> leaderRatings) {
        Stream<? extends AbstractRating> latestRatingsStream = playerRatings.stream();
        Optional<AbstractRating> earliestRatingDate = Stream.concat(latestRatingsStream, leaderRatings.stream())
                .min(Comparator.comparing(AbstractRating::getRatingDate));

        if (earliestRatingDate.isEmpty()) {
            LocalDate earliestDate = matchRepository.findEarliestFinishDate();
            log.debug("{}: no previous ratings detected, earliest date set to {}", LogId.get(), earliestDate);
            return earliestDate;
        }

        log.debug("{}: previous ratings found, earliest date set to {}", LogId.get(), earliestRatingDate);
        return earliestRatingDate.get().getRatingDate().plusDays(1);
    }
}
