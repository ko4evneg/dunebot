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
    private Long telegramChatId;
    private String firstName;
    private String steamName;
    @Nullable
    private String lastName;
    @Nullable
    // TODO:  rename
    private String userName;

    public String getFriendlyName() {

        return String.format("%s (%s)", steamName, getFullName());
    }

    private String getFullName() {
        return lastName == null ? firstName : firstName + " " + lastName;
    }
}
