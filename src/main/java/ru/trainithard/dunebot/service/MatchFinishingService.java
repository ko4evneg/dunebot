package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MatchFinishingService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final TransactionTemplate transactionTemplate;
    private final MessagingService messagingService;
    private final TaskScheduler taskScheduler;

    public void scheduleForceFinishMatch(long matchId, Instant dateTime) {
        Match match = matchRepository.findById(matchId).orElseThrow();
        if (!match.isFinished()) {
            //messagingService.sendMessageAsync(getMatchFinishMessage(match));
        }
    }

    public void finishMatch(long matchId) {
        Match match = matchRepository.findByIdWithMatchPlayers(matchId).orElseThrow();
        match.setFinished(true);
        match.getMatchPlayers().forEach(matchPlayer -> matchPlayer.setPlace(matchPlayer.getCandidatePlace()));
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(match.getMatchPlayers());
        });

        messagingService.sendMessageAsync(getMatchFinishMessage(match));
    }

    private MessageDto getMatchFinishMessage(Match match) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Матч ").append(match.getId()).append(" завершился:\n");
        Map<Integer, String> playerNamesByPlace = new LinkedHashMap<>();
        match.getMatchPlayers().stream()
                .filter(matchPlayer -> Objects.requireNonNull(matchPlayer.getPlace()) != -1)
                .sorted(Comparator.comparing(MatchPlayer::getPlace))
                .forEach(matchPlayer -> playerNamesByPlace.put(matchPlayer.getPlace(), matchPlayer.getPlayer().getFriendlyName()));
        playerNamesByPlace.forEach((place, name) -> stringBuilder.append(place).append(". ").append(name).append("\n"));
        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf("\n"));

        ExternalPollId externalPollId = match.getExternalPollId();
        return new MessageDto(externalPollId.getChatId().toString(), stringBuilder.toString(), externalPollId.getReplyId(), null);
    }
}
