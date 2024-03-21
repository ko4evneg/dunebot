package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StartupServiceImpl implements StartupService {
    private static final String MATCH_FAIL_MESSAGE_TEMPLATE = "Бот был перезапущен, незавершенные матчи %s завершены без регистрации результатов";

    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final SettingsService settingsService;

    @Override
    public void startUp() {
        List<Match> notEndedMatches = matchRepository.findAllByStateIn(List.of(MatchState.NEW, MatchState.ON_SUBMIT));
        Map<Integer, String> matchIdStringsByTopicId = new HashMap<>();
        for (Match match : notEndedMatches) {
            match.setState(MatchState.FAILED);

            Integer chatId = match.getExternalPollId().getReplyId();
            matchIdStringsByTopicId.merge(chatId, match.getId().toString(), (oldVal, val) -> oldVal + ", " + val);
        }
        matchRepository.saveAll(notEndedMatches);

        String chatId = settingsService.getStringSetting(SettingKey.CHAT_ID);
        matchIdStringsByTopicId.forEach((topicId, matchIds) -> {
            ExternalMessage externalMessage = new ExternalMessage(String.format(MATCH_FAIL_MESSAGE_TEMPLATE, "(" + matchIds + ")"));
            MessageDto messageDto = new MessageDto(chatId, externalMessage, topicId, null);
            messagingService.sendMessageAsync(messageDto);
        });
    }
}
