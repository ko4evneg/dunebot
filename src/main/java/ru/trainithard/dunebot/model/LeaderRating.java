package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "LEADER_RATINGS")
@NoArgsConstructor
public class LeaderRating extends Rating {
    @OneToOne
    @JoinColumn(name = "LEADER_ID")
    private Leader leader;
}
