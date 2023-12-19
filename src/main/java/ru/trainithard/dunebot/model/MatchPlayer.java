package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Entity
@Table(name = "match_players")
public class MatchPlayer extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @OneToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @Nullable
    private Integer place;
}
