package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "LEADER_RATINGS")
@NoArgsConstructor
public class LeaderRating extends AbstractRating {
    @OneToOne
    @JoinColumn(name = "LEADER_ID")
    private Leader leader;

    @Override
    public Long getEntityId() {
        return this.getLeader().getId();
    }

    @Override
    void initEntity(MatchPlayer matchPlayer) {
        if (Objects.isNull(this.leader)) {
            this.leader = matchPlayer.getLeader();
        }
    }
}
