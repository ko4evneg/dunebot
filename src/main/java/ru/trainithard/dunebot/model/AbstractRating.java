package ru.trainithard.dunebot.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractRating extends BaseEntity {
    private LocalDate ratingDate;
    private int matchesCount;
    private double efficiency;
    private double winRate;
    private int firstPlaceCount;
    private int secondPlaceCount;
    private int thirdPlaceCount;
    private int fourthPlaceCount;

    public abstract Long getEntityId();
}
