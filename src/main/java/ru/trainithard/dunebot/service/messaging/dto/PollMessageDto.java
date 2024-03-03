package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;

import java.util.List;

@Getter
@NoArgsConstructor
public class PollMessageDto extends MessageDto {
    private List<String> options;

    public PollMessageDto(String chatId, ExternalMessage externalMessage, @Nullable Integer replyMessageId, List<String> options) {
        super(chatId, externalMessage, replyMessageId, null);
        this.options = options;
    }
}
