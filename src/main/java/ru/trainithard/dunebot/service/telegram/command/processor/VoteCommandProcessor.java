package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteCommandProcessor extends CommandProcessor {
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        List<Integer> selectedPollAnswers = commandMessage.getPollVote().selectedAnswerId();
        if (selectedPollAnswers.contains(SettingConstants.POSITIVE_POLL_OPTION_ID)) {
            registerMatchPlayer(commandMessage);
            // TODO:  notify?
        } else {
            unregisterMatchPlayer(commandMessage);
            // TODO:  notify?
        }
    }

    private void registerMatchPlayer(CommandMessage commandMessage) {
        playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player ->
                matchRepository.findByExternalPollIdPollId(commandMessage.getPollVote().pollId())
                        .ifPresent(match -> {
                                    int currentPositiveAnswersCount = match.getPositiveAnswersCount();
                                    match.setPositiveAnswersCount(currentPositiveAnswersCount + 1);
                                    MatchPlayer matchPlayer = new MatchPlayer(match, player);
                                    transactionTemplate.executeWithoutResult(status -> {
                                        matchRepository.save(match);
                                        matchPlayerRepository.save(matchPlayer);
                                    });
                                    if (currentPositiveAnswersCount < match.getModType().getPlayersCount()) {
                                    }
                                    //      if (match.getRegisteredPlayersCount() == 4)
                                    // TODO: start match if threshold crosses 4}
                                }
                        ));
    }

    private void unregisterMatchPlayer(CommandMessage commandMessage) {
        matchPlayerRepository
                .findByMatchExternalPollIdPollIdAndPlayerExternalId(commandMessage.getPollVote().pollId(), commandMessage.getUserId())
                .ifPresent(matchPlayer -> {
                    Match match = matchPlayer.getMatch();
                    int currentPositiveAnswersCount = match.getPositiveAnswersCount();
                    match.setPositiveAnswersCount(currentPositiveAnswersCount - 1);
                    transactionTemplate.executeWithoutResult(status -> {
                        matchRepository.save(match);
                        matchPlayerRepository.delete(matchPlayer);
                    });
                });
        //      if (match.getRegisteredPlayersCount() == 0) {
        // TODO: delete start match if threshold crosses 4});
    }

    @Override
    public Command getCommand() {
        return Command.VOTE;
    }
}
