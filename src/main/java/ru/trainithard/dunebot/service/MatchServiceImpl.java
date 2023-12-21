package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
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
import ru.trainithard.dunebot.service.telegram.TelegramBotAdapter;

import java.util.Optional;

import static ru.trainithard.dunebot.configuration.SettingConstants.CHAT_ID;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
    private final TelegramBotAdapter telegramBotAdapter;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    // TODO:
    private final PlayerRepository playerRepository;
    private final TransactionTemplate transactionTemplate;


    @Override
    public void requestNewMatch(Player initiator, ModType modType) {
        TelegramUserMessageDto telegramUserMessage = telegramBotAdapter.sendPoll(initiator, modType);
        if (telegramUserMessage.getThrowable() == null) {
            transactionTemplate.executeWithoutResult(status -> {
                Match match = new Match(modType);
                match.setTelegramPollId(telegramUserMessage.getTelegramPollId());
                match.setTelegramMessageId(telegramUserMessage.getTelegramMessageId());
                match.setOwner(initiator);
                match.setRegisteredPlayersCount(1);
                Match savedMatch = matchRepository.save(match);
                MatchPlayer matchPlayer = new MatchPlayer(savedMatch, initiator);
                matchPlayerRepository.save(matchPlayer);
            });
        }
    }

    @Override
    public void cancelMatch(long playerId) {
        Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(playerId);
        if (latestOwnedMatchOptional.isPresent()) {
            Match latestOwnedMatch = latestOwnedMatchOptional.get();
            if (latestOwnedMatch.isFinished()) {
                // TODO:  notify
                return;
            }
            telegramBotAdapter.deleteMessage(latestOwnedMatch.getTelegramMessageId(), CHAT_ID, (bool, throwable) ->
                    transactionTemplate.executeWithoutResult(status -> {
                        matchRepository.delete(latestOwnedMatch);
                        matchPlayerRepository.deleteAll(latestOwnedMatch.getMatchPlayers());
                    })
            );
        }
    }

    @Override
    public void registerMathPlayer(Player player, Match match) {
        match.increaseRegisteredPlayerCount();
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        // TODO: remove template?
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.save(matchPlayer);
        });
//      if (match.getRegisteredPlayersCount() == 4)
        // TODO: start match
    }


    @Override
    public void unregisterMathPlayer(MatchPlayer matchPlayer) {
        Match match = matchPlayer.getMatch();
        match.decreaseRegisteredPlayerCount();
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.delete(matchPlayer);
        });
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
