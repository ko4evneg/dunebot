package ru.trainithard.dunebot.service.dto;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.service.telegram.ChatType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Getter
public final class PlayerRegistrationDto {
    private final long telegramId;
    private final long telegramChatId;
    private final String firstName;
    private final String steamName;
    @Nullable
    private final String lastName;
    @Nullable
    private final String userName;
    private final ChatType messageType;

    public PlayerRegistrationDto(CommandMessage commandMessage) {
        this.steamName = commandMessage.getAllArguments();
        this.telegramId = commandMessage.getUserId();
        this.telegramChatId = commandMessage.getChatId();
        this.firstName = commandMessage.getFirstName();
        this.lastName = commandMessage.getLastName();
        this.userName = commandMessage.getUserName();
        this.messageType = commandMessage.getChatType();
    }
}
