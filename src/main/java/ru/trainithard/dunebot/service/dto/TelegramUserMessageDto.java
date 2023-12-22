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
    private Integer telegramMessageId;
    private Long telegramChatId;
    private String telegramPollId;
}
