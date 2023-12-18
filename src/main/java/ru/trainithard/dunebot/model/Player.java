package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Entity
@Table(name = "players")
public class Player extends BaseEntity {
    private Long telegramId;
    private String firstName;
    @Nullable
    private String lastName;
    @Nullable
    private String userName;
}
