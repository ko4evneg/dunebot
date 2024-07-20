package ru.trainithard.dunebot.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@MappedSuperclass
public abstract class Rating extends BaseEntity {
    private LocalDate startDate;
    private LocalDate endDate;
    private int matchesCount;
    private double efficiency;
    private double winRate;
    private int firstPlaceCount;
    private int secondPlaceCount;
    private int thirdPlaceCount;
    private int fourthPlaceCount;
}
