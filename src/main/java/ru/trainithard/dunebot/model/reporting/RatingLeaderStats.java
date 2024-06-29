package ru.trainithard.dunebot.model.reporting;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.trainithard.dunebot.model.Leader;

@Entity
@Getter
@Setter
@Table(name = "RATING_LEADER_STATS")
@NoArgsConstructor
public class RatingLeaderStats extends DatedRatingReport {
    @ManyToOne
    @JoinColumn(name = "LEADER_ID")
    private Leader leader;
}
