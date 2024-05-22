package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "LEADERS")
@NoArgsConstructor
public class Leader extends BaseEntity implements Rateable {
    private String name;
    @Enumerated(EnumType.STRING)
    private ModType modType;

    @Override
    public String getRatingName() {
        return name;
    }
}
