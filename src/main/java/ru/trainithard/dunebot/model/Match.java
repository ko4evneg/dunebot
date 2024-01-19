package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;

import java.util.List;

/**
 * Entity describing specific game match.c
 */
@Entity
@Getter
@Setter
@Table(name = "matches")
@NoArgsConstructor
public class Match extends BaseEntity {
    /**
     * Positively voted players for the match.
     */
    @OneToMany(mappedBy = "match", fetch = FetchType.LAZY)
    private List<MatchPlayer> matchPlayers;
    /**
     * Player who initiated the match poll.
     */
    @OneToOne
    @JoinColumn(name = "owner_id")
    private Player owner;
    /**
     * External ID of the poll created for this match.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "EXTERNAL_POLL_ID")
    private ExternalPollId externalPollId;
    /**
     * External ID of the message sent, when match have enough players to start.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "EXTERNAL_START_ID")
    private ExternalMessageId externalStartId;
    /**
     * Game mode of the match.
     */
    @Enumerated(EnumType.STRING)
    private ModType modType;
    /**
     * Describes whether match is finished.
     */
    @Column(name = "is_finished")
    private boolean finished;
    /**
     * Describes whether submit command was invoked for the match.
     */
    @Column(name = "is_onsubmit")
    private boolean onSubmit;
    /**
     * Count of positive votes in the match poll.
     */
    private int positiveAnswersCount;
    /**
     * Count of accepted submit answers for the match (when user selects his place).
     */
    private int submitsCount;
    /**
     * Count of <b>retries</b> to submit this match (does not include initial submit).
     */
    private int submitsRetryCount;

    public Match(ModType modType) {
        this.modType = modType;
    }

    public boolean areAllSubmitsReceived() {
        return submitsCount == matchPlayers.size();
    }

    public boolean isResubmitAllowed(int resubmitsLimit) {
        return submitsRetryCount < resubmitsLimit;
    }
}
