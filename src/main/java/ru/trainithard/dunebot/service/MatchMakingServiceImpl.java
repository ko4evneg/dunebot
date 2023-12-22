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
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.ConfirmMatchDto;
import ru.trainithard.dunebot.service.dto.MatchSubmitDto;
import ru.trainithard.dunebot.service.dto.TelegramUserMessageDto;

@Service
@RequiredArgsConstructor
public class MatchMakingServiceImpl implements MatchMakingService {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    // TODO:
    private final PlayerRepository playerRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registerNewMatch(Player initiator, ModType modType, TelegramUserMessageDto telegramUserMessage) {
        Match match = new Match(modType);
        match.setTelegramPollId(telegramUserMessage.getTelegramPollId());
        match.setTelegramMessageId(telegramUserMessage.getTelegramMessageId());
        match.setTelegramChatId(telegramUserMessage.getTelegramChatId());
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
    public void registerMathPlayer(Player player, Match match) {
        match.increaseRegisteredPlayerCount();
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        matchRepository.save(match);
        matchPlayerRepository.save(matchPlayer);
//      if (match.getRegisteredPlayersCount() == 4)
        // TODO: start match
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void unregisterMathPlayer(MatchPlayer matchPlayer) {
        Match match = matchPlayer.getMatch();
        match.decreaseRegisteredPlayerCount();
        matchRepository.save(match);
        matchPlayerRepository.delete(matchPlayer);
//      if (match.getRegisteredPlayersCount() == 0) {
        // TODO: start match
    }

    @Override
    public void acceptMatchSubmit(MatchSubmitDto matchSubmit) {

    }

    @Override
    public void confirmMatchSubmit(ConfirmMatchDto confirmMatch) {

    }
}
