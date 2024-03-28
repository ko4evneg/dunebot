package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.trainithard.dunebot.configuration.SettingConstants.EXTERNAL_LINE_SEPARATOR;

/**
 * Accepts player's reply to match submit message (place selection), validates consistency of specific match selected
 * places and run resubmit process for conflicting matches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptSubmitCommandProcessor extends CommandProcessor {
    private static final String RESUBMIT_LIMIT_EXCEEDED_MESSAGE =
            "Игроки не смогли верно обозначить свои места! Превышено количество запросов на регистрацию результатов. Результаты не сохранены, регистрация запрещена.";

    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final ResubmitCommandProcessor resubmitProcessor;
    private final MessagingService messagingService;
    private final SettingsService settingsService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        log.debug("{}: ACCEPT_SUBMIT started", logId());

        Callback callback = new Callback(commandMessage.getCallback());
        Match match = matchRepository.findWithMatchPlayersBy(callback.matchId).orElseThrow();
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();
        MatchPlayer submittingPlayer = getSubmittingPlayer(commandMessage.getUserId(), matchPlayers);
        log.debug("{}: match id: {}, player id: {}", logId(), match.getId(), submittingPlayer.getPlayer().getId());

        if (!submittingPlayer.hasCandidateVote()) {
            deleteOldSubmitMessage(submittingPlayer);

            int candidatePlace = callback.candidatePlace;
            int resubmitsLimit = settingsService.getIntSetting(SettingKey.RESUBMITS_LIMIT);
            log.debug("{}: candidatePlace = {}", logId(), candidatePlace);
            if (isConflictSubmit(match.getMatchPlayers(), candidatePlace) && match.isResubmitAllowed(resubmitsLimit)) {
                log.debug("{}: conflict resolution - resubmit", logId());
                ExternalMessage conflictMessage = getConflictMessage(matchPlayers, submittingPlayer, candidatePlace);
                sendMessagesToMatchPlayers(matchPlayers, conflictMessage);
                resubmitProcessor.process(match, logId());
            } else if (isConflictSubmit(matchPlayers, candidatePlace)) {
                log.debug("{}: conflict resolution - failed match", logId());
                sendMessagesToMatchPlayers(matchPlayers, new ExternalMessage(RESUBMIT_LIMIT_EXCEEDED_MESSAGE));
                ExternalMessage resubmitsLimitExceededMessage = getResubmitsLimitFinishMessage(match.getId());
                matchFinishingService.finishNotSubmittedMatch(match.getId(), resubmitsLimitExceededMessage, logId());
            } else {
                log.debug("{}: player's submit processing", logId());
                submittingPlayer.setCandidatePlace(candidatePlace);
                match.setSubmitsCount(match.getSubmitsCount() + 1);
                transactionTemplate.executeWithoutResult(status -> {
                    matchRepository.save(match);
                    matchPlayerRepository.save(submittingPlayer);
                    log.debug("{}: match {} and submitting matchPlayers saved)", logId(), match.getId());
                });
                Long chatId = submittingPlayer.getSubmitMessageId().getChatId();
                messagingService.sendMessageAsync(new MessageDto(chatId, getSubmitText(match.getId(), candidatePlace), null, null));
                if (match.canBeFinished()) {
                    matchFinishingService.finishSubmittedMatch(match.getId(), logId());
                }
                log.debug("{}: player's submit successfully processed", logId());
            }
        }

        log.debug("{}: ACCEPT_SUBMIT ended", logId());
    }

    private MatchPlayer getSubmittingPlayer(long externalUserId, List<MatchPlayer> matchPlayers) {
        return matchPlayers.stream()
                .filter(mPlayer -> mPlayer.getPlayer().getExternalId() == externalUserId)
                .findFirst().orElseThrow();
    }

    private void deleteOldSubmitMessage(MatchPlayer submittingPlayer) {
        if (submittingPlayer.hasSubmitMessage()) {
            messagingService.deleteMessageAsync(submittingPlayer.getSubmitMessageId());
        }
    }

    private boolean isConflictSubmit(List<MatchPlayer> matchPlayers, int candidatePlace) {
        return matchPlayers.stream()
                .filter(matchPlayer -> {
                    Integer comparedCandidatePlace = matchPlayer.getCandidatePlace();
                    return comparedCandidatePlace != null && comparedCandidatePlace == candidatePlace;
                })
                .peek(matchPlayer -> log.debug("{}: conflict found. matchPlayer {} has same candidatePlace ", logId(), matchPlayer.getId()))
                .findFirst().isPresent();
    }

    private ExternalMessage getConflictMessage(Collection<MatchPlayer> matchPlayers, MatchPlayer candidate, int candidatePlace) {
        Map<Integer, List<MatchPlayer>> playersByPlace = matchPlayers.stream()
                .filter(matchPlayer -> matchPlayer.getCandidatePlace() != null && !matchPlayer.getId().equals(candidate.getId()))
                .collect(Collectors.groupingBy(MatchPlayer::getCandidatePlace));
        List<MatchPlayer> candidatePlaceMatchPlayers = playersByPlace.computeIfAbsent(candidatePlace, key -> new ArrayList<>());
        candidatePlaceMatchPlayers.add(candidate);
        ExternalMessage conflictMessage = new ExternalMessage("Некоторые игроки не смогли поделить ").startBold();
        for (Map.Entry<Integer, List<MatchPlayer>> entry : playersByPlace.entrySet()) {
            List<MatchPlayer> conflictMatchPlayers = entry.getValue();
            if (conflictMatchPlayers.size() > 1) {
                conflictMessage.append(entry.getKey()).append(" место").endBold().append(":").newLine();
                conflictMatchPlayers.stream()
                        .map(matchPlayer -> matchPlayer.getPlayer().getFriendlyName())
                        .forEach(playerFriendlyName -> conflictMessage.append(playerFriendlyName).newLine());
            }
        }
        conflictMessage.append(EXTERNAL_LINE_SEPARATOR).append("Повторный опрос результата...");

        return conflictMessage;
    }

    private ExternalMessage getResubmitsLimitFinishMessage(Long matchId) {
        return new ExternalMessage()
                .startBold().append("Матч ").append(matchId).endBold()
                .append(" завершен без результата, так как превышено максимальное количество попыток регистрации мест");
    }

    private ExternalMessage getSubmitText(long matchId, int candidatePlace) {
        return candidatePlace == 1 ?
                getAcceptedFirstPlaceSubmitMessageTemplate(matchId, candidatePlace) :
                getAcceptedSubmitMessageTemplate(matchId, candidatePlace);
    }

    private ExternalMessage getAcceptedFirstPlaceSubmitMessageTemplate(long matchId, int candidatePlace) {
        return getAcceptedSubmitMessageTemplate(matchId, candidatePlace).newLine()
                .append("Теперь загрузите в этот чат скриншот победы.");
    }

    private ExternalMessage getAcceptedSubmitMessageTemplate(long matchId, int candidatePlace) {
        return new ExternalMessage("В матче ").append(matchId).append(" за вами зафиксировано ")
                .startBold().append(candidatePlace).append(" место").endBold().append(".").newLine()
                .append("При ошибке используйте команду '/resubmit ").append(matchId).append("'.");
    }

    private void sendMessagesToMatchPlayers(Collection<MatchPlayer> matchPlayers, ExternalMessage message) {
        for (MatchPlayer matchPlayer : matchPlayers) {
            messagingService.sendMessageAsync(new MessageDto(matchPlayer.getPlayer().getExternalChatId(), message, null, null));
        }
    }

    @Override
    public Command getCommand() {
        return Command.ACCEPT_SUBMIT;
    }

    private static class Callback {

        private final long matchId;
        private final int candidatePlace;

        private Callback(String callbackText) {
            String[] callbackData = callbackText.split("__");
            this.matchId = Long.parseLong(callbackData[0]);
            this.candidatePlace = Integer.parseInt(callbackData[1]);
        }
    }
}
