package ru.trainithard.dunebot.service.messaging;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PollMessageDto {
    private final boolean allowMultipleAnswers = false;
    private final boolean anonymous = false;
    private String text;
    private String chatId;
    private int replyMessageId;
    private List<String> options;
}
