package ru.trainithard.dunebot.service.report.v2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.model.AbstractRating;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;

import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.trainithard.dunebot.model.AbstractRating.RATING_COMPARATOR;

@Slf4j
//todo lock simultaneous execution
public abstract class RatingUpdateService<T extends AbstractRating> {
    private final Function<MatchPlayer, Long> entityIdSupplier = getEntityIdSupplier();

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Takes matches and ratingDateInfo for the same period and calculate all resulting ratings.
     * <b>VERY IMPORTANT NOTE:</b> matches and rating must be selected from the same period, otherwise result is not guaranteed.
     *
     * @param matches       matches for specific period (<b>MUST</b> be the same period as for <code>latestRatings</code>)
     * @param latestRatings latest PlayerRating collection specific period (<b>MUST</b> be the same period as for <code>matches</code>)
     */
    void updateRatings(Collection<Match> matches, Collection<T> latestRatings) {
        Map<YearMonth, Map<Long, T>> latestRatingByEntityIdByMonth = latestRatings.stream()
                .collect(Collectors.groupingBy(
                        rating -> YearMonth.from(rating.getRatingDate()),
                        Collectors.toMap(T::getEntityId, Function.identity()))
                );

        Map<YearMonth, Map<Long, List<MatchPlayer>>> matchPlayersByEntityIdByMonth = matches.stream()
                .collect(Collectors.groupingBy(
                        match -> YearMonth.from(match.getFinishDate()),
                        Collectors.flatMapping(
                                match -> match.getMatchPlayers().stream()
                                        .filter(MatchPlayer::hasRateablePlace),
                                Collectors.groupingBy(entityIdSupplier)))
                );

        matchPlayersByEntityIdByMonth.forEach((month, monthMatchPlayersById) -> {
            Map<Long, T> monthRatingsById = latestRatingByEntityIdByMonth.getOrDefault(month, Collections.emptyMap());
            List<T> updatedRatings = getUpdatedRatings(monthMatchPlayersById, monthRatingsById);
            if (!updatedRatings.isEmpty()) {
                transactionTemplate.executeWithoutResult(status -> saveRatings(updatedRatings));
            }
        });
    }

    private List<T> getUpdatedRatings(Map<Long, List<MatchPlayer>> matchPlayersById, Map<Long,T> ratingsById) {
        List<T> updatedRatings = new ArrayList<>();
        matchPlayersById.forEach((eId, matchPlayers) -> {
            T latestRating = ratingsById.get(eId);
            List<MatchPlayer> effectiveMatchPlayers = matchPlayers.stream()
                    .sorted(RATING_COMPARATOR)
                    .filter(MatchPlayer::hasRateablePlace)
                    .filter(matchPlayer -> Objects.isNull(latestRating) || isRatingDateBeforeMatchDate(matchPlayer, latestRating))
                    .toList();
            if (!effectiveMatchPlayers.isEmpty()) {
                T rating = Objects.requireNonNullElse(latestRating, createNewRating(matchPlayers.get(0)));
                rating.consume(effectiveMatchPlayers);
                updatedRatings.add(rating);
            }
        });
        return updatedRatings;
    }

    private boolean isRatingDateBeforeMatchDate(MatchPlayer matchPlayer, T latestRating) {
        return latestRating.getRatingDate().isBefore(matchPlayer.getMatch().getFinishDate());
    }

    abstract Function<MatchPlayer, Long> getEntityIdSupplier();

    abstract T createNewRating(MatchPlayer matchPlayer);

    abstract void saveRatings(Collection<T> ratings);
}
