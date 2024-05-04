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

/**
 * Entity represents registered player.
 */
@Getter
@Setter
@Entity
@Table(name = "PLAYERS")
@NoArgsConstructor
public class Player extends BaseEntity {
    /**
     * User ID in external messaging system
     */
    private long externalId;
    /**
     * Chat ID in external messaging system
     */
    private long externalChatId;
    /**
     * Player's first name
     */
    private String firstName;
    /**
     * Player's steam nickname
     */
    private String steamName;
    /**
     * Player's last name
     */
    private String lastName;
    /**
     * Player's first name in external messaging system
     */
    private String externalFirstName;
    /**
     * Player's alias in external messaging system (for telegram: @alias)
     */
    @Nullable
    private String externalName;
    /**
     * Shows if player was registered using /profile command or by voting in rating poll.
     */
    @Column(name = "IS_GUEST")
    private boolean guest;
    /**
     * Shows if player has blocked chat, so he can't be used for sending messages.
     */
    @Column(name = "IS_CHAT_BLOCKED")
    private boolean chatBlocked;

    private Player(CommandMessage commandMessage, ParsedNames parsedNames) {
        this.externalId = commandMessage.getUserId();
        this.externalChatId = commandMessage.getChatId();
        this.externalFirstName = commandMessage.getExternalFirstName();
        this.firstName = parsedNames.getFirstName();
        this.lastName = parsedNames.getLastName();
        this.steamName = parsedNames.getSteamName();
        this.externalName = commandMessage.getUserName();
    }

    private Player(CommandMessage commandMessage, int guestId) {
        this.externalId = commandMessage.getUserId();
        this.externalChatId = commandMessage.getChatId();
        this.externalFirstName = commandMessage.getExternalFirstName();
        this.externalName = commandMessage.getUserName();
        this.guest = true;
        this.firstName = "Vasya";
        this.lastName = "Pupkin";
        this.steamName = "guest" + guestId;
    }

    public static Player createRegularPlayer(CommandMessage commandMessage, ParsedNames parsedNames) {
        return new Player(commandMessage, parsedNames);
    }

    public static Player createGuestPlayer(CommandMessage commandMessage, int guestId) {
        return new Player(commandMessage, guestId);
    }

    public String getFriendlyName() {

        return String.format("%s (%s) %s", firstName, steamName, lastName);
    }

    public String getMentionTag() {
        return externalName == null ? externalFirstName : externalName;
    }
}
