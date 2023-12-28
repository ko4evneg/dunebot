package ru.trainithard.dunebot.service.messaging;

import ru.trainithard.dunebot.model.messaging.ExternalPollId;

import java.util.concurrent.CompletableFuture;

public interface MessagingService {
    CompletableFuture<TelegramPollDto> sendPollAsync(PollMessageDto pollMessage);

    void deletePollAsync(ExternalPollId externalPollId);
}
