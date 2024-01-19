package ru.trainithard.dunebot.model.messaging;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.model.BaseEntity;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;

/**
 * Class representing text message in external messenger.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "EXTERNAL_MESSAGES")
@Getter
@Setter
@Embeddable
@NoArgsConstructor
public class ExternalMessageId extends BaseEntity {
    protected Integer messageId;
    protected Long chatId;
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
