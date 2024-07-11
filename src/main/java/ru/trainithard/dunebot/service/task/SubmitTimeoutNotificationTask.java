package ru.trainithard.dunebot.service.task;

import lombok.extern.slf4j.Slf4j;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

@Slf4j
public class SubmitTimeoutNotificationTask implements DunebotRunnable {
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final ExternalMessageFactory messageFactory;
    private final AppSettingsService appSettingsService;
    private final long matchId;

    public SubmitTimeoutNotificationTask(MatchRepository matchRepository, MessagingService messagingService,
                                         ExternalMessageFactory messageFactory, AppSettingsService appSettingsService, long matchId) {
        this.matchRepository = matchRepository;
        this.messagingService = messagingService;
        this.messageFactory = messageFactory;
        this.appSettingsService = appSettingsService;
        this.matchId = matchId;
    }

    @Override
    public void run() {
        matchRepository.findWithMatchPlayersBy(matchId).ifPresent(match -> {
            log.debug("0: match {} found in state {}", matchId, match.getState());
            if (match.getState() == MatchState.ON_SUBMIT) {
                int timeout = appSettingsService.getIntSetting(AppSettingKey.SUBMIT_TIMEOUT_WARNING_NOTIFICATION);
                log.debug("0: submit notification timeout received: {} minutes", timeout);
                ExternalMessage externalMessage = messageFactory.getPreSubmitTimeoutNotificationMessage(match, timeout);
                MessageDto message = new MessageDto(match.getExternalPollId(), externalMessage);
                messagingService.sendMessageAsync(message);
            }
        });
    }
}
