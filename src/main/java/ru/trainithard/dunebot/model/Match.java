package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "matches")
public class Match extends BaseEntity {
    @OneToMany(mappedBy = "match")
    private List<PlayerResult> playerResults;
    @Enumerated
    private ModType modType;
}
