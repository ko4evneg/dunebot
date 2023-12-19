package ru.trainithard.dunebot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "matches")
public class Match extends BaseEntity {
    @OneToMany(mappedBy = "match", fetch = FetchType.LAZY)
    private List<MatchPlayer> matchPlayers;
    @OneToOne
    @JoinColumn(name = "owner_id")
    private Player owner;
    private String telegramPollId;
    private int telegramMessageId;
    @Enumerated(EnumType.STRING)
    private ModType modType;
}
