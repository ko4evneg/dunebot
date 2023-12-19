package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Entity
@Table(name = "player_matches")
public class PlayerMatch extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    @OneToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @Nullable
    private Integer place;
}
