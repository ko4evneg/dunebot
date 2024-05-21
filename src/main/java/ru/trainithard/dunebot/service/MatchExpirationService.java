package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchExpirationService {
    private static final int EXPIRATION_TIMEOUT = 12 * 60;
    private final MatchRepository matchRepository;
    private final Clock clock;

    public void expireUnusedMatches() {
        log.debug("0: match expiration service started...");
        List<Match> newMatches = matchRepository.findAllByStateIn(Collections.singleton(MatchState.NEW));
        log.debug("0: found {} matches with NEW state", newMatches.size());
        LocalDateTime now = LocalDateTime.now(clock);
        newMatches.forEach(match -> {
            Instant expirationInstant = match.getCreatedAt().plus(EXPIRATION_TIMEOUT, ChronoUnit.MINUTES);
            LocalDateTime expirationTime = expirationInstant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            if (now.isAfter(expirationTime) && match.hasMissingPlayers()) {
                match.setState(MatchState.EXPIRED);
                log.debug("0: expiring match {}", match.getId());
            }
        });
        matchRepository.saveAll(newMatches);
        log.debug("0: expired matches saved");
    }
}
