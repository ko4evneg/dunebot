package ru.trainithard.dunebot.model;

import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class AbstractRating extends BaseEntity {
    private static Comparator<MatchPlayer> ratingComparator = Comparator
            .comparing((MatchPlayer matchPlayer) -> matchPlayer.getMatch().getFinishDate())
            .thenComparing(MatchPlayer::getId);

    LocalDate ratingDate;
    int matchesCount;
    double efficiency;
    double winRate;
    int firstPlaceCount;
    int secondPlaceCount;
    int thirdPlaceCount;
    int fourthPlaceCount;

    AbstractRating(LocalDate ratingDate) {
        this.ratingDate = ratingDate;
    }

    public abstract Long getEntityId();

    void calculateSpecificFields(MatchPlayer matchPlayer) {
    }

    public void consume(Collection<MatchPlayer> matchPlayers) {
        matchPlayers.stream()
                .filter(matchPlayer -> matchPlayer.getMatch().getState() == MatchState.FINISHED)
                .filter(this::matchPlayerBelongsThisRatingPeriod)
                .sorted(ratingComparator)
                .forEach(this::consume);
    }

    private boolean matchPlayerBelongsThisRatingPeriod(MatchPlayer matchPlayer) {
        return YearMonth.from(matchPlayer.getMatch().getFinishDate()).equals(YearMonth.from(ratingDate));
    }

    private void consume(MatchPlayer matchPlayer) {
        if (matchPlayer.hasRateablePlace()) {
            int place = Objects.requireNonNull(matchPlayer.getPlace());
            incrementPlaceCount(place);
            matchesCount++;
            efficiency = RatingCalculator.calculateEfficiency(this);
            winRate = (double) firstPlaceCount / matchesCount;
            calculateSpecificFields(matchPlayer);
        }
        LocalDate matchDate = matchPlayer.getMatch().getFinishDate();
        if (matchDate.isAfter(ratingDate)) {
            ratingDate = matchDate;
        }
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
