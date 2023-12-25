package ru.trainithard.dunebot.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Message;

@Setter
@Getter
@NoArgsConstructor
// TODO:  remove
@AllArgsConstructor
public final class TelegramUserMessageDto {
    private Integer telegramMessageId;
    private Long telegramChatId;
    private String telegramPollId;

    public TelegramUserMessageDto(Message message) {
        this.telegramMessageId = message.getMessageId();
        this.telegramChatId = message.getChatId();
        this.telegramPollId = message.getPoll().getId();
    }
}
