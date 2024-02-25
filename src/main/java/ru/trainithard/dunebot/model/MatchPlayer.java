package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;

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
     * External ID of a message sent to a player for selection of his <code>candidatePlace</code>.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "EXTERNAL_SUBMIT_ID")
    private ExternalMessageId submitMessageId;
    /**
     * Place in the <code>Match</code>. Is set when all players sent their <code>candidatePlace</code> without conflicts.
     * May have value of integer between 1 and max number of match participants, or <code>null</code> if not set.
     */
    @Nullable
    private Integer place;
    /**
     * Candidate place in the <code>Match</code>. Used as a buffer before setting actual <code>place</code> for resolving
     * conflicting places. May have value of integer between 1 and max number of match participants, or <code>null</code> if not set.
     */
    @Nullable
    private Integer candidatePlace;

    public MatchPlayer(Match match, Player player) {
        this.match = match;
        this.player = player;
    }

    public boolean hasCandidateVote() {
        return candidatePlace != null;
    }

    public boolean hasSubmitMessage() {
        return submitMessageId != null;
    }
}
