package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "PLAYER_RATINGS")
@NoArgsConstructor
public class PlayerRating extends AbstractRating {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLAYER_ID")
    private Player player;
    private int currentStrikeLength;
    private int maxStrikeLength;
    private boolean previouslyWon;

    @Override
    public Long getEntityId() {
        return this.getPlayer().getId();
    }

    @Override
    void initEntity(MatchPlayer matchPlayer) {
        if (Objects.isNull(this.player)) {
            this.player = matchPlayer.getPlayer();
        }
    }

    @Override
    void calculateSpecificFields(MatchPlayer matchPlayer) {
        int matchPlace = Objects.requireNonNull(matchPlayer.getPlace());
        if (matchPlace == 1) {
            this.currentStrikeLength++;
            this.previouslyWon = true;
            if (this.maxStrikeLength < this.currentStrikeLength) {
                maxStrikeLength++;
            }
        } else {
            this.currentStrikeLength = 0;
            this.previouslyWon = false;
        }
    }
}
