package ru.trainithard.dunebot.model.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;

@MappedSuperclass
@Getter
@Setter
@Embeddable
@NoArgsConstructor
public class ExternalMessageId {
    @Column(name = "EXTERNAL_MESSAGE_ID")
    protected Integer messageId;
    @Column(name = "EXTERNAL_CHAT_ID")
    protected Long chatId;
    @Column(name = "EXTERNAL_REPLY_ID")
    private Integer replyId;

    public ExternalMessageId(int messageId, long chatId, @Nullable Integer replyId) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.replyId = replyId;
    }

    public ExternalMessageId(ExternalMessageDto messageDto) {
        this.messageId = messageDto.getMessageId();
        this.chatId = messageDto.getChatId();
        this.replyId = messageDto.getReplyId();
    }

    public String getChatIdString() {
        return Long.toString(chatId);
    }
}
