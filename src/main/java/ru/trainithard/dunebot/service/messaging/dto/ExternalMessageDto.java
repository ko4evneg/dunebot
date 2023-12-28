package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Message;

@Setter
@Getter
@NoArgsConstructor
public class ExternalMessageDto {
    protected Integer messageId;
    protected Long chatId;
    protected Integer replyId;

    public ExternalMessageDto(Message message) {
        this.messageId = message.getMessageId();
        this.chatId = message.getChatId();
        // TODO:   check
        this.replyId = message.getReplyToMessage().getMessageId();
    }
}
