package ru.trainithard.dunebot.model.reporting;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class RatingEntityReport extends DatedRatingReport {
    private int matchesCount;
    private double efficiency;
    private double winRate;
}
