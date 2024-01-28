package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.trainithard.dunebot.configuration.SettingConstants.EXTERNAL_LINE_SEPARATOR;
import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

@Service
@RequiredArgsConstructor
public class MatchFinishingService {
    private static final Logger logger = LoggerFactory.getLogger(MatchFinishingService.class);
    private static final Set<MatchState> finishedMatchStates = EnumSet.of(MatchState.FAILED, MatchState.FINISHED);

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final TransactionTemplate transactionTemplate;
    private final MessagingService messagingService;
    private final Clock clock;

    public void finishUnsuccessfullySubmittedMatch(long matchId, String reason, int loggingId) {
        logger.debug("{}: unsuccessful_match_finish started", loggingId);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        if (!finishedMatchStates.contains(match.getState())) {
            logger.debug("{}: unsuccessful_match_finish found unfinished match id: {}", loggingId, match.getId());

            if (hasAllPlacesSubmitted(match) && match.hasSubmitPhoto()) {
                logger.debug("{}: unsuccessful_match_finish successfully save started", loggingId);
                finishSuccessfullyAndSave(match);
            } else {
                logger.debug("{}: unsuccessful_match_finish failed scenario save started", loggingId);

                match.setState(MatchState.FAILED);
                match.setFinishDate(LocalDate.now(clock));
                match.getMatchPlayers().forEach(matchPlayer -> matchPlayer.setCandidatePlace(null));
                transactionTemplate.executeWithoutResult(status -> {
                    matchRepository.save(match);
                    matchPlayerRepository.saveAll(match.getMatchPlayers());
                });

                ExternalPollId externalPollId = match.getExternalPollId();
                MessageDto finishMessage = new MessageDto(externalPollId, MarkdownEscaper.getEscaped(reason));
                messagingService.sendMessageAsync(finishMessage);
            }
        }

        logger.debug("{}: unsuccessful_match_finish ended", loggingId);
    }

    private boolean hasAllPlacesSubmitted(Match match) {
        int requiredPlaceSubmits = match.getModType().getPlayersCount();
        Set<Integer> possibleMatchPlaces = IntStream.range(1, requiredPlaceSubmits + 1).boxed().collect(Collectors.toSet());
        match.getMatchPlayers().forEach(matchPlayer -> possibleMatchPlaces.remove(matchPlayer.getCandidatePlace()));
        return possibleMatchPlaces.isEmpty();
    }

    public void finishSuccessfullySubmittedMatch(long matchId, int loggingId) {
        logger.debug("{}: successful_match_finish started", loggingId);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        finishSuccessfullyAndSave(match);

        logger.debug("{}: successful_match_finish ended", loggingId);
    }

    private void finishSuccessfullyAndSave(Match match) {
        match.setState(MatchState.FINISHED);
        match.setFinishDate(LocalDate.now(clock));
        match.getMatchPlayers().forEach(matchPlayer -> matchPlayer.setPlace(matchPlayer.getCandidatePlace()));
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(match.getMatchPlayers());
        });
        messagingService.sendMessageAsync(getMatchFinishMessage(match));
    }

    private MessageDto getMatchFinishMessage(Match match) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("*Матч ").append(match.getId()).append("* завершился:")
                .append(EXTERNAL_LINE_SEPARATOR).append(EXTERNAL_LINE_SEPARATOR);
        Map<Integer, String> playerNamesByPlace = new LinkedHashMap<>();
        match.getMatchPlayers().stream()
                .filter(matchPlayer -> matchPlayer.getPlace() != null &&
                        matchPlayer.getPlace() != NOT_PARTICIPATED_MATCH_PLACE)
                .sorted(Comparator.comparing(MatchPlayer::getPlace))
                .forEach(matchPlayer -> playerNamesByPlace.put(matchPlayer.getPlace(), matchPlayer.getPlayer().getFriendlyName()));
        playerNamesByPlace.forEach((place, name) -> stringBuilder.append(getPlaceEmoji(place)).append(" ").append(name).append(EXTERNAL_LINE_SEPARATOR));
        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(EXTERNAL_LINE_SEPARATOR));

        ExternalPollId externalPollId = match.getExternalPollId();
        return new MessageDto(externalPollId, MarkdownEscaper.getEscaped(stringBuilder.toString()));
    }

    private String getPlaceEmoji(Integer place) {
        return switch (place) {
            case 1 -> "1️⃣";
            case 2 -> "2️⃣";
            case 3 -> "3️⃣";
            case 4 -> "4️⃣";
            case 5 -> "5️⃣";
            case 6 -> "6️⃣";
            default -> "";
        };
    }
}
