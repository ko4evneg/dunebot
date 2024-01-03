package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.List;

@Getter
@NoArgsConstructor
public class MessageDto {
    protected String text;
    protected String chatId;
    protected Integer replyMessageId;
    protected List<List<ButtonDto>> keyboard;

    public MessageDto(String chatId, String text, @Nullable Integer replyMessageId, @Nullable List<List<ButtonDto>> linedButtons) {
        this.text = text;
        this.chatId = chatId;
        this.replyMessageId = replyMessageId;
        this.keyboard = linedButtons;
    }
}
