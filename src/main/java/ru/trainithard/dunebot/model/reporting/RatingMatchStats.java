package ru.trainithard.dunebot.model.reporting;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "RATING_MATCH_STATS")
@NoArgsConstructor
public class RatingMatchStats extends DatedRatingReport {
    private int totalPlayersMatchesCount;
    private int failedPlayersMatchesCount;
    private int succeedPlayersMatchesCount;
    private int totalLeadersMatchesCount;
    private int failedLeadersMatchesCount;
    private int succeedLeadersMatchesCount;
}
