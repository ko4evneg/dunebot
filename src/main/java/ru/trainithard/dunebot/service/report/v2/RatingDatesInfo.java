package ru.trainithard.dunebot.service.report.v2;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.model.AbstractRating;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RatingDatesInfo<T extends AbstractRating> {
    private final Map<Long, T> ratingsById = new HashMap<>();
    @Getter
    @Nullable
    private final LocalDate earliestRatingDate;

    public RatingDatesInfo(Collection<T> latestRatings) {
        LocalDate earliestDate = null;
        for (T rating : latestRatings) {
            Long entityId = rating.getEntityId();
            LocalDate ratingDate = rating.getRatingDate();
            ratingsById.merge(entityId, rating, (oldRating, init) ->
                    rating.getRatingDate().isAfter(oldRating.getRatingDate()) ? rating : oldRating);
            if (earliestDate == null || ratingDate.isBefore(earliestDate)) {
                earliestDate = ratingDate;
            }
        }
        this.earliestRatingDate = earliestDate;
    }

    public Optional<T> getLatestRatingsById(long rateableId) {
        return Optional.ofNullable(ratingsById.get(rateableId));
    }
}
