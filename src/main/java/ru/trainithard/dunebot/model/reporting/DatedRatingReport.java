package ru.trainithard.dunebot.model.reporting;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import ru.trainithard.dunebot.model.BaseEntity;

import java.time.LocalDate;

@Getter
@Setter
@MappedSuperclass
public class DatedRatingReport extends BaseEntity {
    private LocalDate startDate;
    private LocalDate endDate;
}
