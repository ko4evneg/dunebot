package ru.trainithard.dunebot.service.task;

import lombok.extern.slf4j.Slf4j;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

@Slf4j
public class SubmitTimeoutNotificationTask implements DunebotRunnable {
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final ExternalMessageFactory messageFactory;
    private final long matchId;

    public SubmitTimeoutNotificationTask(MatchRepository matchRepository, MessagingService messagingService,
                                         ExternalMessageFactory messageFactory, long matchId) {
        this.matchRepository = matchRepository;
        this.messagingService = messagingService;
        this.messageFactory = messageFactory;
        this.matchId = matchId;
    }

    @Override
    public void run() {
        log.debug("PRE_TIMEOUT_NOTIFICATION match {} finishing started", matchId);
        matchRepository.findWithMatchPlayersBy(matchId).ifPresent(match -> {
            ExternalMessage externalMessage = messageFactory.getPreSubmitTimeoutNotificationMessage(match);
            if (!externalMessage.getText().isBlank()) {
                MessageDto message = new MessageDto(match.getExternalPollId(), externalMessage);
                messagingService.sendMessageAsync(message);
            }
        });
    }
}
