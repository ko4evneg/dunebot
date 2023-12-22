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

    private String telegramPollId;
    private Integer telegramMessageId;
    private Long telegramChatId;
    private int registeredPlayersCount;

    public Match(ModType modType) {
        this.modType = modType;
    }

    public void increaseRegisteredPlayerCount() {
        registeredPlayersCount++;
    }

    public void decreaseRegisteredPlayerCount() {
        registeredPlayersCount--;
    }
}
