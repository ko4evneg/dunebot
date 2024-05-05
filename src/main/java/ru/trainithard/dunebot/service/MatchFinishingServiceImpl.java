package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.factory.ExternalMessageFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import static ru.trainithard.dunebot.configuration.SettingConstants.EXTERNAL_LINE_SEPARATOR;
import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchFinishingServiceImpl implements MatchFinishingService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final TransactionTemplate transactionTemplate;
    private final MessagingService messagingService;
    private final Clock clock;
    private final ExternalMessageFactory externalMessageFactory;

    @Override
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void finishNotSubmittedMatch(long matchId, boolean isFailedByResubmitsLimit) {
        int logId = LogId.get();
        log.debug("{}: finishing not submitted match {} started", logId, matchId);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        if (MatchState.getEndedMatchStates().contains(match.getState())) {
            log.debug("{}: Match {} has been ended already. Finishing is not required. Exiting...", logId, matchId);
            return;
        }
        log.debug("{}: match {} found (state: {}, photo: {})", logId, match.getId(), match.getState(), match.hasSubmitPhoto());
        if (!MatchState.getEndedMatchStates().contains(match.getState())) {
            if (match.hasAllPlacesSubmitted() && match.hasSubmitPhoto()) {
                finishSuccessfullyAndSave(match);
            } else {
                ExternalMessage finishReasonMessage = externalMessageFactory.getFinishReasonMessage(match, isFailedByResubmitsLimit);
                failMatch(match);
                transactionTemplate.executeWithoutResult(status -> {
                    matchRepository.save(match);
                    matchPlayerRepository.saveAll(match.getMatchPlayers());
                    log.debug("{}: match {} and its matchPlayers saved", logId, match.getId());
                });

                messagingService.sendMessageAsync(new MessageDto(match.getExternalPollId(), finishReasonMessage));
            }
        }
        log.debug("{}: finishing not submitted match ended", logId);
    }

    private void failMatch(Match match) {
        match.setState(MatchState.FAILED);
        match.setFinishDate(LocalDate.now(clock));
        match.getMatchPlayers().forEach(matchPlayer -> matchPlayer.setCandidatePlace(null));
    }

    @Override
    public void finishSubmittedMatch(long matchId) {
        log.debug("{}: finishing submitted match started", LogId.get());
        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        if (MatchState.getEndedMatchStates().contains(match.getState())) {
            throw new IllegalStateException("Can't accept submit for match " + match.getId() + " due to its ended state");
        }
        finishSuccessfullyAndSave(match);
        log.debug("{}: finishing submitted match ended", LogId.get());
    }

    private void finishSuccessfullyAndSave(Match match) {
        log.debug("{}: processing successful finish", LogId.get());
        match.setState(MatchState.FINISHED);
        match.setFinishDate(LocalDate.now(clock));
        match.getMatchPlayers().forEach(matchPlayer -> matchPlayer.setPlace(matchPlayer.getCandidatePlace()));
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(match.getMatchPlayers());
            log.debug("{}: match {} and its matchPlayers saved", LogId.get(), match.getId());
        });
        messagingService.sendMessageAsync(getMatchFinishMessage(match));
    }

    private MessageDto getMatchFinishMessage(Match match) {
        ExternalMessage message = new ExternalMessage();
        message.startBold().append("Матч ").append(match.getId()).endBold().append(" завершился:")
                .append(EXTERNAL_LINE_SEPARATOR).append(EXTERNAL_LINE_SEPARATOR);

        Map<Integer, String> playerNamesByPlace = new LinkedHashMap<>();
        match.getMatchPlayers().stream()
                .filter(matchPlayer -> matchPlayer.getPlace() != null &&
                                       matchPlayer.getPlace() != NOT_PARTICIPATED_MATCH_PLACE)
                .sorted(Comparator.comparing(MatchPlayer::getPlace))
                .forEach(matchPlayer -> playerNamesByPlace.put(matchPlayer.getPlace(), matchPlayer.getPlayer().getFriendlyName()));
        playerNamesByPlace.forEach((place, name) ->
                message.append(getPlaceEmoji(place)).append(" ").append(name).append(EXTERNAL_LINE_SEPARATOR));

        ExternalPollId externalPollId = match.getExternalPollId();
        return new MessageDto(externalPollId, message);
    }

    private String getPlaceEmoji(Integer place) {
        return switch (place) {
            case 1 -> "1️⃣";
            case 2 -> "2️⃣";
            case 3 -> "3️⃣";
            case 4 -> "4️⃣";
            case 5 -> "5️⃣";
            case 6 -> "6️⃣";
            default -> throw new IllegalArgumentException("Can't determine place number emoji");
        };
    }
}
