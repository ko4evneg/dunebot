package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "PLAYER_RATINGS")
@NoArgsConstructor
public class PlayerRating extends AbstractRating {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLAYER_ID")
    private Player player;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LAST_STRIKE_MATCH_ID")
    private Match lastStrikeMatch;
    private int strikeLength;
}
