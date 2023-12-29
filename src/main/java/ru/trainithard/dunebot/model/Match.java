package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;

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
    private ExternalPollId externalPollId;
    private int positiveAnswersCount;
    private int submitsCount;

    public Match(ModType modType) {
        this.modType = modType;
    }
}
