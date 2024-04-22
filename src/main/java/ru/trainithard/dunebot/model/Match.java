package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;

import java.time.LocalDate;
import java.util.List;

/**
 * Entity describing specific game match.c
 */
@Entity
@Getter
@Setter
@Table(name = "MATCHES")
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
    @JoinColumn(name = "OWNER_ID")
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
     * Leader, who won this match.
     */
    @OneToOne
    @JoinColumn(name = "LEADER_WON")
    private Leader leaderWon;
    /**
     * Game mode of the match.
     */
    @Enumerated(EnumType.STRING)
    private ModType modType;
    /**
     * Describes whether match is finished.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private MatchState state;
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
    /**
     * Path of the screenshot for the match
     */
    private String screenshotPath;
    /**
     * Finish match date.
     */
    private LocalDate finishDate;

    public Match(ModType modType) {
        this.modType = modType;
        this.state = MatchState.NEW;
    }

    public boolean hasSubmitPhoto() {
        return screenshotPath != null;
    }

    public boolean canBeFinished() {
        return submitsCount == matchPlayers.size() && screenshotPath != null;
    }

    public boolean isResubmitAllowed(int resubmitsLimit) {
        return submitsRetryCount < resubmitsLimit;
    }

    public boolean isFull() {
        return modType.getPlayersCount() == positiveAnswersCount;
    }

    public boolean hasMissingPlayers() {
        return modType.getPlayersCount() > positiveAnswersCount;
    }

    public boolean hasEnoughPlayers() {
        return positiveAnswersCount >= modType.getPlayersCount();
    }

    public void prepareForResubmit() {
        submitsRetryCount++;
        submitsCount = 0;
        state = MatchState.ON_SUBMIT;
        leaderWon = null;
    }
}
