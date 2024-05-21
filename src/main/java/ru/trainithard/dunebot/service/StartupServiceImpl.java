package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartupServiceImpl implements StartupService {
    private static final String MATCH_FAIL_MESSAGE_TEMPLATE =
            "Бот был перезапущен, незавершенные матчи %s завершены без регистрации результатов";

    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final AppSettingsService appSettingsService;
    private final MatchExpirationService expirationService;

    @Override
    public void startUp() {
        log.info("Startup match validation...");
        expirationService.expireUnusedMatches();

        List<Match> notEndedMatches = matchRepository.findAllByStateNotIn(MatchState.getEndedMatchStates());
        Map<Integer, String> matchIdStringsByTopicId = new HashMap<>();
        for (Match match : notEndedMatches) {
            log.debug("Set failing state for match {}", match.getId());
            match.setState(MatchState.CANCELLED);
            Integer chatId = match.getExternalPollId().getReplyId();
            matchIdStringsByTopicId.merge(chatId, match.getId().toString(), (oldVal, val) -> oldVal + ", " + val);
        }
        matchRepository.saveAll(notEndedMatches);
        log.debug("All failed matches saved");

        String chatId = appSettingsService.getStringSetting(AppSettingKey.CHAT_ID);
        if (chatId != null) {
            matchIdStringsByTopicId.forEach((topicId, matchIds) -> {
                ExternalMessage externalMessage = new ExternalMessage(String.format(MATCH_FAIL_MESSAGE_TEMPLATE, "(" + matchIds + ")"));
                MessageDto messageDto = new MessageDto(chatId, externalMessage, topicId, null);
                messagingService.sendMessageAsync(messageDto);
            });
        }
        log.info("Startup match validation finished");
    }
}
