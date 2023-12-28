package ru.trainithard.dunebot.service.messaging;

import ru.trainithard.dunebot.model.messaging.ExternalPollId;

import java.util.concurrent.CompletableFuture;

public interface MessagingService {
    CompletableFuture<ExternalPollDto> sendPollAsync(PollMessageDto pollMessage);

    void deletePollAsync(ExternalPollId externalPollId);
}
