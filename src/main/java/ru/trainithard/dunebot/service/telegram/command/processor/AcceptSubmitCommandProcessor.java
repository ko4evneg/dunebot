package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
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
import static ru.trainithard.dunebot.configuration.SettingConstants.RESUBMITS_LIMIT;

@Service
@RequiredArgsConstructor
public class AcceptSubmitCommandProcessor extends CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AcceptSubmitCommandProcessor.class);
    private static final String UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE =
            "*Матч %d* завершен без результата, так как превышено максимальное количество попыток регистрации мест";
    private static final String ACCEPTED_SUBMIT_MESSAGE_TEMPLATE =
            "В матче %1$d за вами зафиксировано %2$d место.%3$sПри ошибке используйте команду '/resubmit %1$d'";
    private static final String ACCEPTED_FIRST_PLACE_SUBMIT_MESSAGE_TEMPLATE =
            "В матче %1$d за вами зафиксировано %2$d место.%3$sПри ошибке используйте команду '/resubmit %1$d'." +
                    EXTERNAL_LINE_SEPARATOR + "Теперь загрузите в этот чат скриншот победы.";
    private static final String RESUBMIT_LIMIT_EXCEEDED_MESSAGE =
            "Игроки не смогли верно обозначить свои места! Превышено количество запросов на регистрацию результатов. Результаты не сохранены, регистрация запрещена.";

    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final ResubmitCommandProcessor resubmitProcessor;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: accept_submit started", loggingId);

        Callback callback = new Callback(commandMessage.getCallback());
        Match match = matchRepository.findWithMatchPlayersBy(callback.matchId).orElseThrow();
        List<MatchPlayer> matchPlayers = match.getMatchPlayers();
        MatchPlayer submittingPlayer = getSubmittingPlayer(commandMessage.getUserId(), matchPlayers);

        logger.debug("{}: set match id: {}, player id: {}", loggingId, match.getId(), submittingPlayer.getPlayer().getId());

        if (!submittingPlayer.hasCandidateVote()) {
            deleteOldSubmitMessage(submittingPlayer);

            int candidatePlace = callback.candidatePlace;
            if (isConflictSubmit(match.getMatchPlayers(), candidatePlace) && match.isResubmitAllowed(RESUBMITS_LIMIT)) {
                logger.debug("{}: not exceeding resubmits conflict resolution started", loggingId);

                String conflictText = getConflictMessage(matchPlayers, submittingPlayer, candidatePlace);
                sendMessagesToMatchPlayers(matchPlayers, conflictText);
                resubmitProcessor.process(match, loggingId);

                logger.debug("{}: not exceeding resubmits conflict resolution ended successfully", loggingId);
            } else if (isConflictSubmit(matchPlayers, candidatePlace)) {
                logger.debug("{}: exceeding resubmits conflict successfully ended", loggingId);

                sendMessagesToMatchPlayers(matchPlayers, RESUBMIT_LIMIT_EXCEEDED_MESSAGE);
                matchFinishingService.finishUnsuccessfullySubmittedMatch(match.getId(), String.format(UNSUCCESSFUL_SUBMIT_MATCH_FINISH_MESSAGE, match.getId()), loggingId);

                logger.debug("{}: exceeding resubmits conflict resolution successfully ended", loggingId);
            } else {
                logger.debug("{}: not conflicting submit processing started", loggingId);

                submittingPlayer.setCandidatePlace(candidatePlace);
                match.setSubmitsCount(match.getSubmitsCount() + 1);
                transactionTemplate.executeWithoutResult(status -> {
                    matchRepository.save(match);
                    matchPlayerRepository.save(submittingPlayer);
                });
                Long chatId = submittingPlayer.getSubmitMessageId().getChatId();
                messagingService.sendMessageAsync(new MessageDto(chatId, getSubmitText(match, candidatePlace), null, null));
                if (match.canBeFinished()) {
                    matchFinishingService.finishSuccessfullySubmittedMatch(match.getId(), loggingId);
                }

                logger.debug("{}: not conflicting submit processing successfully ended", loggingId);
            }
        }

        logger.debug("{}: accept_submit ended", loggingId);
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
                .anyMatch(matchPlayer -> {
                    Integer comparedCandidatePlace = matchPlayer.getCandidatePlace();
                    return comparedCandidatePlace != null && comparedCandidatePlace == candidatePlace;
                });
    }

    private String getConflictMessage(Collection<MatchPlayer> matchPlayers, MatchPlayer candidate, int candidatePlace) {
        Map<Integer, List<MatchPlayer>> playersByPlace = matchPlayers.stream()
                .filter(matchPlayer -> matchPlayer.getCandidatePlace() != null && !matchPlayer.getId().equals(candidate.getId()))
                .collect(Collectors.groupingBy(MatchPlayer::getCandidatePlace));
        List<MatchPlayer> candidatePlaceMatchPlayers = playersByPlace.computeIfAbsent(candidatePlace, key -> new ArrayList<>());
        candidatePlaceMatchPlayers.add(candidate);
        StringBuilder conflictTextBuilder = new StringBuilder("Некоторые игроки не смогли поделить *");
        for (Map.Entry<Integer, List<MatchPlayer>> entry : playersByPlace.entrySet()) {
            List<MatchPlayer> conflictMatchPlayers = entry.getValue();
            if (conflictMatchPlayers.size() > 1) {
                conflictTextBuilder.append(entry.getKey()).append(" место*:").append(EXTERNAL_LINE_SEPARATOR);
                conflictMatchPlayers.stream()
                        .map(matchPlayer -> matchPlayer.getPlayer().getFriendlyName())
                        .forEach(playerFriendlyName -> conflictTextBuilder.append(playerFriendlyName).append(EXTERNAL_LINE_SEPARATOR));
            }
        }
        conflictTextBuilder.append(EXTERNAL_LINE_SEPARATOR).append("Повторный опрос результата...");

        return conflictTextBuilder.toString();
    }

    private String getSubmitText(Match match, int candidatePlace) {
        return candidatePlace == 1 ?
                String.format(ACCEPTED_FIRST_PLACE_SUBMIT_MESSAGE_TEMPLATE, match.getId(), candidatePlace, EXTERNAL_LINE_SEPARATOR) :
                String.format(ACCEPTED_SUBMIT_MESSAGE_TEMPLATE, match.getId(), candidatePlace, EXTERNAL_LINE_SEPARATOR);
    }

    private void sendMessagesToMatchPlayers(Collection<MatchPlayer> matchPlayers, String message) {
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
