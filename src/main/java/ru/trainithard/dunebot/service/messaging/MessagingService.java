package ru.trainithard.dunebot.service.messaging;

import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.ExternalPollDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;

import java.util.concurrent.CompletableFuture;

public interface MessagingService {
    CompletableFuture<ExternalPollDto> sendPollAsync(PollMessageDto pollMessage);

    CompletableFuture<ExternalMessageDto> sendMessageAsync(MessageDto message);

    void deleteMessageAsync(ExternalMessageId externalMessageId);
}
