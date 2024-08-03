package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchCreationServiceImpl implements MatchCreationService {
    private static final String POSITIVE_ANSWER = "Да";
    private static final List<String> POLL_OPTIONS = List.of(POSITIVE_ANSWER, "Нет", "Результат");
    private static final String NEW_POLL_MESSAGE_TEMPLATE = "Игрок %s призывает всех на матч в %s";

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final AppSettingsService appSettingsService;

    @Override
    public void createMatch(long creatorExternalId, ModType modType) {
        int logId = LogId.get();
        playerRepository.findByExternalId(creatorExternalId)
                .ifPresent(player -> {
                    messagingService.sendPollAsync(getNewPollMessage(player, modType))
                            .thenAccept(telegramPollDto -> {
                                log.debug("{}: match creation request callback received", logId);
                                Match match = new Match(modType);
                                match.setExternalPollId(telegramPollDto.toExternalPollId());
                                match.setOwner(player);
                                Match savedMatch = matchRepository.save(match);
                                log.debug("{}: new match {} saved", logId, savedMatch.getId());
                            });
                    log.debug("{}: match creation request sent", logId);
                });
    }

    private PollMessageDto getNewPollMessage(Player initiator, ModType modType) {
        String text = String.format(NEW_POLL_MESSAGE_TEMPLATE, initiator.getFriendlyName(), modType.getModName());
        String chatId = appSettingsService.getStringSetting(AppSettingKey.CHAT_ID);
        ExternalMessage externalMessage = new ExternalMessage().appendRaw(text);
        return new PollMessageDto(chatId, externalMessage, getTopicId(modType), POLL_OPTIONS);
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_CLASSIC);
            case UPRISING_4, UPRISING_6, BUFF -> appSettingsService.getIntSetting(AppSettingKey.TOPIC_ID_UPRISING);
        };
    }
}
