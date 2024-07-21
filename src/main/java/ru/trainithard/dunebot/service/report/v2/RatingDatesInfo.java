package ru.trainithard.dunebot.service.report.v2;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.model.RatingDate;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;

public class RatingDatesInfo {
    private final HashMap<Long, LocalDate> latestRatingDatesById = new HashMap<>();
    @Getter
    @Nullable
    private final LocalDate earliestRatingDate;

    public RatingDatesInfo(Collection<RatingDate> latestRatings) {
        LocalDate earliestDate = null;
        for (RatingDate ratingDate : latestRatings) {
            Long playerId = ratingDate.getEntityId();
            LocalDate maxDate = ratingDate.getMaxDate();
            latestRatingDatesById.merge(playerId, maxDate, (oldVal, initVal) ->
                    maxDate.isAfter(oldVal) ? maxDate : oldVal);
            if (earliestDate == null || maxDate.isBefore(earliestDate)) {
                earliestDate = maxDate;
            }
        }
        this.earliestRatingDate = earliestDate;
    }

    public LocalDate getLatestRatingDate(long id) {
        return latestRatingDatesById.get(id);
    }
}
