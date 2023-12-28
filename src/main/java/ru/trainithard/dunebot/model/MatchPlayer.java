package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Entity
@Table(name = "match_players")
@NoArgsConstructor
public class MatchPlayer extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @OneToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @Nullable
    private Integer place;
    @Nullable
    private Integer candidatePlace;

    public MatchPlayer(Match match, Player player) {
        this.match = match;
        this.player = player;
    }
}
