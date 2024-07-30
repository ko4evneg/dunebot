package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "PLAYER_RATINGS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerRating extends AbstractRating {
    @OneToOne
    @JoinColumn(name = "PLAYER_ID")
    private Player player;
    private int currentStrikeLength;
    private int maxStrikeLength;
    @Column(name = "IS_PREVIOUSLY_WON")
    private boolean previouslyWon;

    public PlayerRating(Player player, LocalDate ratingDate) {
        super(ratingDate);
        this.player = player;
    }

    @Override
    public Long getEntityId() {
        return this.getPlayer().getId();
    }

    @Override
    void calculateSpecificFields(MatchPlayer matchPlayer) {
        int matchPlace = Objects.requireNonNull(matchPlayer.getPlace());
        if (matchesCount == 0) {
            this.maxStrikeLength = 0;
        }
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
