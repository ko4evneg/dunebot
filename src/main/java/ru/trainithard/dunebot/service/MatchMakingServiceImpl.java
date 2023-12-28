package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalPollDto;

@Service
@RequiredArgsConstructor
public class MatchMakingServiceImpl implements MatchMakingService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerNewMatch(Player initiator, ModType modType, ExternalPollDto telegramUserMessage) {
        Match match = new Match(modType);
        match.setExternalPollId(telegramUserMessage.toExternalPollId());
        match.setOwner(initiator);
        Match savedMatch = matchRepository.save(match);
        MatchPlayer matchPlayer = new MatchPlayer(savedMatch, initiator);
        matchPlayerRepository.save(matchPlayer);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelMatch(Match latestOwnedMatch) {
        matchRepository.delete(latestOwnedMatch);
        matchPlayerRepository.deleteAll(latestOwnedMatch.getMatchPlayers());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerMathPlayer(Player player, Match match, int positiveAnswersCount) {
        match.setPositiveAnswersCount(positiveAnswersCount);
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        matchRepository.save(match);
        matchPlayerRepository.save(matchPlayer);
//      if (match.getRegisteredPlayersCount() == 4)
        // TODO: start match if threshold crosses 4
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void unregisterMathPlayer(MatchPlayer matchPlayer, int positiveAnswersCount) {
        Match match = matchPlayer.getMatch();
        match.setPositiveAnswersCount(positiveAnswersCount);
        matchRepository.save(match);
        matchPlayerRepository.delete(matchPlayer);
//      if (match.getRegisteredPlayersCount() == 0) {
        // TODO: delete start match if threshold crosses 4
    }
}
