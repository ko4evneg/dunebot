package ru.trainithard.dunebot.model.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@MappedSuperclass
@Getter
@Setter
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
}
