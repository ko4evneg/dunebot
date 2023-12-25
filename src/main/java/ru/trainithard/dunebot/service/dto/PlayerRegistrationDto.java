package ru.trainithard.dunebot.service.dto;

import lombok.Getter;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

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

    public PlayerRegistrationDto(Message message, String steamName) {
        this.steamName = steamName;
        User telegramUser = message.getFrom();
        this.telegramId = telegramUser.getId();
        this.telegramChatId = message.getChatId();
        this.firstName = telegramUser.getFirstName();
        this.lastName = telegramUser.getLastName();
        this.userName = telegramUser.getUserName();
    }
}
