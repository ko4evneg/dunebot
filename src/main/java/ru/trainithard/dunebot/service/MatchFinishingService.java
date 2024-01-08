package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.repository.MatchRepository;

@Service
@RequiredArgsConstructor
public class MatchFinishingService {
    private final MatchRepository matchRepository;

    public void finishMatch(long matchId) {

    }
}
