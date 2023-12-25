package ru.trainithard.dunebot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TelegramMessageId {
    @Column(name = "TELEGRAM_MESSAGE_ID")
    private Integer messageId;
    @Column(name = "TELEGRAM_CHAT_ID")
    private Long chatId;
}
