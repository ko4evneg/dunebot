package ru.trainithard.dunebot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.util.ParsedNames;

@Getter
@Setter
@Entity
@Table(name = "PLAYERS")
@NoArgsConstructor
public class Player extends BaseEntity {
    private long externalId;
    private long externalChatId;
    private String firstName;
    private String steamName;
    private String lastName;
    private String externalFirstName;
    @Nullable
    private String externalName;
    @Column(name = "IS_GUEST")
    private boolean guest;

    public Player(CommandMessage commandMessage, ParsedNames parsedNames) {
        this.externalId = commandMessage.getUserId();
        this.externalChatId = commandMessage.getChatId();
        this.externalFirstName = commandMessage.getExternalFirstName();
        this.firstName = parsedNames.getFirstName();
        this.lastName = parsedNames.getLastName();
        this.steamName = parsedNames.getSteamName();
        this.externalName = commandMessage.getUserName();
    }

    public String getFriendlyName() {

        return String.format("%s (%s) %s", firstName, steamName, lastName);
    }

    public String getMention() {
        return externalName == null ? "@" + externalFirstName : "@" + externalName;
    }
}
