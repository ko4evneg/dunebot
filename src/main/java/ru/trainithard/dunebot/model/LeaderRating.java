package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "LEADER_RATINGS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaderRating extends AbstractRating {
    @OneToOne
    @JoinColumn(name = "LEADER_ID")
    private Leader leader;

    public LeaderRating(Leader leader, LocalDate ratingDate) {
        super(ratingDate);
        this.leader = leader;
    }

    @Override
    public Long getEntityId() {
        return this.getLeader().getId();
    }
}
