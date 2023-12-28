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
    private long externalId;
    private long externalChatId;
    private String firstName;
    private String steamName;
    @Nullable
    private String lastName;
    @Nullable
    private String externalName;

    public String getFriendlyName() {

        return String.format("%s (%s)", steamName, getFullName());
    }

    private String getFullName() {
        return lastName == null ? firstName : firstName + " " + lastName;
    }
}
