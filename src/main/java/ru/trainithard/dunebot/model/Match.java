package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "matches")
@NoArgsConstructor
public class Match extends BaseEntity {
    @OneToMany(mappedBy = "match", fetch = FetchType.LAZY)
    private List<MatchPlayer> matchPlayers;

    @OneToOne
    @JoinColumn(name = "owner_id")
    private Player owner;

    @Enumerated(EnumType.STRING)
    private ModType modType;

    @Column(name = "is_finished")
    private boolean finished;

    @Column(name = "is_onsubmit")
    private boolean onSubmit;

    @Embedded
    private TelegramMessageId telegramMessageId;

    private String telegramPollId;
    private int positiveAnswersCount;

    public Match(ModType modType) {
        this.modType = modType;
    }
}
