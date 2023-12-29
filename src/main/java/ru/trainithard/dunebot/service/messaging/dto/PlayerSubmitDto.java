package ru.trainithard.dunebot.service.messaging.dto;

import org.telegram.telegrambots.meta.api.objects.Message;

public record PlayerSubmitDto(long externalUserId, String submitValue) {
    public PlayerSubmitDto(Message message) {
        this(message.getFrom().getId(), message.getText());
    }
}
