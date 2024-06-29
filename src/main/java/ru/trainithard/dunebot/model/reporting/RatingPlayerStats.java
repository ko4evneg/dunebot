package ru.trainithard.dunebot.model.reporting;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.trainithard.dunebot.model.Player;

@Entity
@Getter
@Setter
@Table(name = "RATING_PLAYER_STATS")
@NoArgsConstructor
public class RatingPlayerStats extends DatedRatingReport {
    @ManyToOne
    @JoinColumn(name = "PLAYER_ID")
    private Player player;
}
