package ru.trainithard.dunebot.service.messaging;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;

@Setter
@Getter
@NoArgsConstructor
public final class TelegramPollDto {
    private Integer messageId;
    private Long chatId;
    private String pollId;
    private Integer replyId;

    public TelegramPollDto(Message message) {
        this.messageId = message.getMessageId();
        this.chatId = message.getChatId();
        this.pollId = message.getPoll().getId();
        this.replyId = message.getMessageThreadId();
    }

    public ExternalPollId toExternalPollId() {
        return new ExternalPollId(messageId, chatId, pollId, replyId);
    }
}
