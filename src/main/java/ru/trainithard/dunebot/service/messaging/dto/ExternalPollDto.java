package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;

@Setter
@Getter
@NoArgsConstructor
public final class ExternalPollDto extends ExternalMessageDto {
    private String pollId;

    public ExternalPollDto(Message message) {
        super(message);
        this.pollId = message.getPoll().getId();
    }

    public ExternalPollId toExternalPollId() {
        return new ExternalPollId(messageId, chatId, pollId, replyId);
    }
}
