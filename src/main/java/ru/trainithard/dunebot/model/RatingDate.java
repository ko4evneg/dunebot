package ru.trainithard.dunebot.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class RatingDate {
    private Long entityId;
    private LocalDate maxDate;
}
