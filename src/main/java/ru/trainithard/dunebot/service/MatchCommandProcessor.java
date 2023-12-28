package ru.trainithard.dunebot.service;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.MatchSubmitDto;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchCommandProcessor {
    private final MatchMakingService matchMakingService;
    private final MatchSubmitService matchSubmitService;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;

    private static final List<String> POLL_OPTIONS = List.of("Да", "Нет", "Результат");

    public void registerNewMatch(long externalUserId, ModType modType) {
        playerRepository.findByExternalId(externalUserId)
                .ifPresent(player -> messagingService.sendPollAsync(getNewPoll(player, modType))
                        .thenAccept(telegramPollDto -> matchMakingService.registerNewMatch(player, modType, telegramPollDto)));
    }

    private PollMessageDto getNewPoll(Player initiator, ModType modType) {
        String text = "Игрок " + initiator.getFriendlyName() + " призывает всех на матч в " + modType.getModName();
        return new PollMessageDto(SettingConstants.CHAT_ID, text, getTopicId(modType), POLL_OPTIONS);
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> SettingConstants.TOPIC_ID_CLASSIC;
            case UPRISING_4, UPRISING_6 -> SettingConstants.TOPIC_ID_UPRISING;
        };
    }

    public void cancelMatch(long externalUserId) {
        playerRepository.findByExternalId(externalUserId).ifPresent(player -> {
            Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(player.getId());
            if (latestOwnedMatchOptional.isPresent()) {
                Match latestOwnedMatch = latestOwnedMatchOptional.get();
                if (latestOwnedMatch.isFinished()) {
                    throw new AnswerableDuneBotException("Запрещено отменять завершенные матчи!", player.getExternalChatId());
                }
                messagingService.deletePollAsync(latestOwnedMatch.getExternalPollId());
                matchMakingService.cancelMatch(latestOwnedMatch);
            }
        });
    }

    public void registerMathPlayer(TelegramUserPollDto pollMessage) {
        playerRepository.findByExternalId(pollMessage.telegramUserId()).ifPresent(player ->
                matchRepository.findByExternalPollIdPollId(pollMessage.telegramPollId()).ifPresent(match ->
                        matchMakingService.registerMathPlayer(player, match, pollMessage.positiveAnswersCount())
                )
        );
    }

    public void unregisterMathPlayer(TelegramUserPollDto pollMessage) {
        matchPlayerRepository
                .findByMatchExternalPollIdPollIdAndPlayerExternalId(pollMessage.telegramPollId(), pollMessage.telegramUserId())
                .ifPresent(matchPlayer -> matchMakingService.unregisterMathPlayer(matchPlayer, pollMessage.positiveAnswersCount()));
    }

    public void registerNewPlayer(PlayerRegistrationDto playerRegistration) {
        playerService.registerNewPlayer(playerRegistration);
    }

    public void getSubmitMessage(long telegramUserId, long telegramChatId, String matchIdString) {
        MatchSubmitDto matchSubmit = matchSubmitService.getMatchSubmit(telegramUserId, telegramChatId, matchIdString);
        List<ButtonDto> buttons = new ArrayList<>();
        List<Player> matchActivePlayers = matchSubmit.activePlayers();
        for (int i = 0; i < matchActivePlayers.size(); i++) {
            ButtonDto buttonDto = new ButtonDto(Integer.toString(i + 1), matchIdString + "__" + (i + 1));
            buttons.add(buttonDto);
        }
        buttons.add(new ButtonDto("не участвовал(а)", matchIdString + "__-1"));
        List<List<ButtonDto>> linedButtons = Lists.partition(buttons, 2);

        String text = "Выберите место, которое вы заняли в матче " + matchSubmit.matchId() + ":";
        matchActivePlayers.forEach(externalId -> messagingService
                .sendMessageAsync(new MessageDto(Long.toString(externalId.getExternalChatId()), text, null, linedButtons)));
    }
}
