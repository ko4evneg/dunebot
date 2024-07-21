package ru.trainithard.dunebot.service.report.v2;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.model.PlayerRating;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;

public class RatingDatesInfo {
    private final HashMap<Long, LocalDate> latestRatingDatesById = new HashMap<>();
    @Getter
    @Nullable
    private final LocalDate earliestRatingDate;

    public RatingDatesInfo(Collection<PlayerRating> latestRatings) {
        LocalDate earliestDate = null;
        for (PlayerRating playerRating : latestRatings) {
            Long playerId = playerRating.getPlayer().getId();
            LocalDate ratingDate = playerRating.getRatingDate();
            latestRatingDatesById.merge(playerId, ratingDate, (oldDate, initVal) ->
                    ratingDate.isAfter(oldDate) ? ratingDate : oldDate);
            if (earliestDate == null || ratingDate.isBefore(earliestDate)) {
                earliestDate = ratingDate;
            }
        }
        this.earliestRatingDate = earliestDate;
    }

    public LocalDate getLatestRatingDate(long id) {
        return latestRatingDatesById.get(id);
    }
}
