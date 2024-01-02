package ru.trainithard.dunebot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Getter
@Setter
@Entity
@Table(name = "players")
@NoArgsConstructor
public class Player extends BaseEntity {
    private long externalId;
    private long externalChatId;
    private String firstName;
    private String steamName;
    @Nullable
    private String lastName;
    @Nullable
    private String externalName;

    public Player(CommandMessage commandMessage) {
        this.externalId = commandMessage.getUserId();
        this.externalChatId = commandMessage.getChatId();
        this.firstName = commandMessage.getFirstName();
        this.lastName = commandMessage.getLastName();
        this.externalName = commandMessage.getUserName();
        this.steamName = commandMessage.getAllArguments();
    }

    public String getFriendlyName() {

        return String.format("%s (%s)", steamName, getFullName());
    }

    private String getFullName() {
        return lastName == null ? firstName : firstName + " " + lastName;
    }
}
