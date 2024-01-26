package ru.trainithard.dunebot.service.messaging;

import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.service.messaging.dto.*;

import java.util.concurrent.CompletableFuture;

public interface MessagingService {
    CompletableFuture<ExternalPollDto> sendPollAsync(PollMessageDto pollMessage);

    CompletableFuture<ExternalMessageDto> sendMessageAsync(MessageDto message);

    CompletableFuture<ExternalMessageDto> sendFileAsync(FileMessageDto fileMessage);

    void deleteMessageAsync(ExternalMessageId externalMessageId);

    CompletableFuture<TelegramFileDetailsDto> getFileDetails(String fileId);
}
