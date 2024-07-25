package ru.trainithard.dunebot.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import ru.trainithard.dunebot.service.report.RatingCalculator;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractRating extends BaseEntity {
    private static Comparator<MatchPlayer> ratingComparator = Comparator
            .comparing((MatchPlayer matchPlayer) -> matchPlayer.getMatch().getFinishDate())
            .thenComparing(MatchPlayer::getId);

    private LocalDate ratingDate;
    private int matchesCount;
    private double efficiency;
    private double winRate;
    private int firstPlaceCount;
    private int secondPlaceCount;
    private int thirdPlaceCount;
    private int fourthPlaceCount;

    public abstract Long getEntityId();

    abstract void initEntity(MatchPlayer matchPlayer);

    void calculateSpecificFields(MatchPlayer matchPlayer) {
    }

    public void consume(Collection<MatchPlayer> matchPlayers) {
        matchPlayers.stream()
                .filter(matchPlayer -> matchPlayer.getMatch().getState() == MatchState.FINISHED)
                .filter(MatchPlayer::hasRateablePlace)
                .filter(this::matchPlayerBelongsThisRatingPeriod)
                .sorted(ratingComparator)
                .forEach(this::consume);
    }

    private boolean matchPlayerBelongsThisRatingPeriod(MatchPlayer matchPlayer) {
        return YearMonth.from(matchPlayer.getMatch().getFinishDate()).equals(YearMonth.from(ratingDate));
    }

    private void consume(MatchPlayer matchPlayer) {
        initEntity(matchPlayer);
        int place = Objects.requireNonNull(matchPlayer.getPlace());
        incrementPlaceCount(place);
        matchesCount++;
        efficiency = RatingCalculator.calculateEfficiency(this);
        winRate = (double) firstPlaceCount / matchesCount;
        calculateSpecificFields(matchPlayer);
    }

    private void incrementPlaceCount(int place) {
        if (place == 1) {
            firstPlaceCount++;
        }
        if (place == 2) {
            secondPlaceCount++;
        }
        if (place == 3) {
            thirdPlaceCount++;
        }
        if (place == 4) {
            fourthPlaceCount++;
        }
    }
}
