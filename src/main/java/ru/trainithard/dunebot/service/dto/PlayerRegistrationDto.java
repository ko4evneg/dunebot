package ru.trainithard.dunebot.service.dto;

import lombok.Getter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.service.telegram.ChatType;
import ru.trainithard.dunebot.service.telegram.command.MessageCommand;

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

    public PlayerRegistrationDto(MessageCommand messageCommand) {
        this.steamName = messageCommand.getArgument(1);
        this.telegramId = messageCommand.getTelegramUserId();
        this.telegramChatId = messageCommand.getTelegramChatId();
        this.firstName = messageCommand.getTelegramFirstName();
        this.lastName = messageCommand.getTelegramLastName();
        this.userName = messageCommand.getTelegramUserName();
        this.messageType = messageCommand.getChatType();
    }
}
