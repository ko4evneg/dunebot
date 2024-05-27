package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchFinishingServiceImpl implements MatchFinishingService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final TransactionTemplate transactionTemplate;
    private final MessagingService messagingService;
    private final Clock clock;
    private final ExternalMessageFactory messageFactory;

    @Override
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void finishNotSubmittedMatch(long matchId, boolean isFailedByResubmitsLimit) {
        int logId = LogId.get();
        log.debug("{}: match {} (not_submitted) finishing...", logId, matchId);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        log.debug("{}: match {} state: {}", logId, matchId, LogId.getMatchLogInfo(match));
        if (MatchState.getEndedMatchStates().contains(match.getState())) {
            log.debug("{}: Match {} has been ended already. Finishing is not required. Exiting...", logId, matchId);
            return;
        }
        log.debug("{}: match {} found (photo: {}, state: {})", logId, match.getId(), match.hasSubmitPhoto(), match.getState());
        if (!MatchState.getEndedMatchStates().contains(match.getState())) {
            assignMissingPlace(match);
            if (match.hasAllPlacesSubmitted() && match.hasSubmitPhoto()) {
                finishSuccessfullyAndSave(match);
            } else {
                ExternalMessage finishReasonMessage = messageFactory.getFinishReasonMessage(match, isFailedByResubmitsLimit);
                failMatch(match);
                transactionTemplate.executeWithoutResult(status -> {
                    matchRepository.save(match);
                    matchPlayerRepository.saveAll(match.getMatchPlayers());
                    log.debug("{}: transaction succeed", logId);
                });

                messagingService.sendMessageAsync(new MessageDto(match.getExternalPollId(), finishReasonMessage));
            }
        }
        log.debug("{}: finishing not submitted match ended", logId);
    }

    private void assignMissingPlace(Match match) {
        List<MatchPlayer> missingCandidatePlacePlayers = match.getMatchPlayers().stream()
                .filter(matchPlayer -> Objects.isNull(matchPlayer.getCandidatePlace()))
                .toList();
        int logId = LogId.get();
        log.debug("{}: found {} matchPlayers without a place", logId, missingCandidatePlacePlayers.size());
        Set<Integer> missingCandidatePlaces = match.getMissingCandidatePlaces();
        if (missingCandidatePlacePlayers.size() == 1 && missingCandidatePlaces.size() == 1) {
            int missingCandidatePlace = missingCandidatePlaces.iterator().next();
            if (missingCandidatePlace != 1) {
                MatchPlayer missingPlaceMatchPlayer = missingCandidatePlacePlayers.get(0);
                missingPlaceMatchPlayer.setCandidatePlace(missingCandidatePlace);
                matchPlayerRepository.save(missingPlaceMatchPlayer);
                log.debug("{}: match {} player {} auto-assigned with {} candidatePlace", logId,
                        match.getId(), missingPlaceMatchPlayer.getPlayer().getId(), missingCandidatePlace);
            }
        }
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
        log.debug("{}: match {} state: {}", LogId.get(), matchId, LogId.getMatchLogInfo(match));
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
            log.debug("{}: transaction succeed", LogId.get());
        });
        ExternalMessage matchSuccessfulFinishMessage = messageFactory.getMatchSuccessfulFinishMessage(match);
        messagingService.sendMessageAsync(new MessageDto(match.getExternalPollId(), matchSuccessfulFinishMessage));
    }
}
