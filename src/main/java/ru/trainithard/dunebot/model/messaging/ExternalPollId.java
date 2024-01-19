package ru.trainithard.dunebot.model.messaging;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

/**
 * Class representing poll in external messenger.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ExternalPollId extends ExternalMessageId {
    private String pollId;

    public ExternalPollId(int messageId, long chatId, String pollId, @Nullable Integer replyId) {
        super(messageId, chatId, replyId);
        this.pollId = pollId;
    }
}
