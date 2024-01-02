package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;

@Service
@RequiredArgsConstructor
public class MatchMakingServiceImpl implements MatchMakingService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelMatch(Match latestOwnedMatch) {
        matchRepository.delete(latestOwnedMatch);
        matchPlayerRepository.deleteAll(latestOwnedMatch.getMatchPlayers());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerMathPlayer(Player player, Match match) {
        int currentPositiveAnswersCount = match.getPositiveAnswersCount();
        match.setPositiveAnswersCount(currentPositiveAnswersCount + 1);
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        matchRepository.save(match);
        matchPlayerRepository.save(matchPlayer);
        if (currentPositiveAnswersCount < match.getModType().getPlayersCount()) {
        }
//      if (match.getRegisteredPlayersCount() == 4)
        // TODO: start match if threshold crosses 4}
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void unregisterMathPlayer(MatchPlayer matchPlayer) {
        Match match = matchPlayer.getMatch();
        int currentPositiveAnswersCount = match.getPositiveAnswersCount();
        match.setPositiveAnswersCount(currentPositiveAnswersCount - 1);
        matchRepository.save(match);
        matchPlayerRepository.delete(matchPlayer);
//      if (match.getRegisteredPlayersCount() == 0) {
        // TODO: delete start match if threshold crosses 4
    }
}
