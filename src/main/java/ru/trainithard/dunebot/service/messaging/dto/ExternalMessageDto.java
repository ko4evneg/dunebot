package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Message;

@Setter
@Getter
@NoArgsConstructor
public class ExternalMessageDto {
    Integer messageId;
    Long chatId;
    Integer replyId;

    public ExternalMessageDto(Message message) {
        this.messageId = message.getMessageId();
        this.chatId = message.getChatId();
        if (message.getReplyToMessage() != null) {
            this.replyId = message.getReplyToMessage().getMessageId();
        }
    }
}
