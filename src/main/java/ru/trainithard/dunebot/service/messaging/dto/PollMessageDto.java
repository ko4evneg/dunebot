package ru.trainithard.dunebot.service.messaging.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.List;

@Getter
@NoArgsConstructor
public class PollMessageDto extends MessageDto {
    private final boolean allowMultipleAnswers = false;
    private final boolean anonymous = false;
    private List<String> options;

    public PollMessageDto(String chatId, String text, @Nullable Integer replyMessageId, List<String> options) {
        super(chatId, text, replyMessageId, null);
        this.options = options;
    }
}
