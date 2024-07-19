package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

/**
 * Entity for relation between specific <code>Match</code> and positively voted <code>Player</code>.
 */
@Getter
@Setter
@Entity
@Table(name = "MATCH_PLAYERS")
@NoArgsConstructor
public class MatchPlayer extends BaseEntity {
    /**
     * Participated <code>Match</code>.
     */
    @ManyToOne
    @JoinColumn(name = "MATCH_ID")
    private Match match;
    /**
     * Positively voted to match poll <code>Player</code>.
     */
    @OneToOne
    @JoinColumn(name = "PLAYER_ID")
    private Player player;
    /**
     * Leader selected by Player in this Match
     */
    @OneToOne
    @JoinColumn(name = "LEADER")
    private Leader leader;
    /**
     * Place in the <code>Match</code>. May have value of integer between 1 and max number of match participants,
     * or <code>null</code> if not set.
     */
    @Nullable
    private Integer place;

    public MatchPlayer(Match match, Player player) {
        this.match = match;
        this.player = player;
    }

    public boolean hasRateablePlace() {
        return place != null && place != NOT_PARTICIPATED_MATCH_PLACE;
    }

    public void resetSubmitData() {
        place = null;
        leader = null;
    }
}
