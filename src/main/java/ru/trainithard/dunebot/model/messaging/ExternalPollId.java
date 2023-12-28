package ru.trainithard.dunebot.model.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ExternalPollId extends ExternalMessageId {
    @Column(name = "EXTERNAL_POLL_ID")
    private String pollId;

    public ExternalPollId(int messageId, long chatId, String pollId, @Nullable Integer replyId) {
        super(messageId, chatId, replyId);
        this.pollId = pollId;
    }
}
