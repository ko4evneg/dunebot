package ru.trainithard.dunebot.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class TelegramUserMessageDto {
    private int telegramMessageId;
    private String telegramPollId;
    private Throwable throwable;
}
