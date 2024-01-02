package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.MatchMakingService;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VoteCommandProcessor implements CommandProcessor {
    private final MatchMakingService matchMakingService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        List<Integer> selectedPollAnswers = commandMessage.getPollVote().selectedAnswerId();
        if (selectedPollAnswers.contains(SettingConstants.POSITIVE_POLL_OPTION_ID)) {
            playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player ->
                    matchRepository.findByExternalPollIdPollId(commandMessage.getPollVote().pollId()).ifPresent(match ->
                            matchMakingService.registerMathPlayer(player, match)));
            // TODO:  notify?
        } else {
            matchPlayerRepository
                    .findByMatchExternalPollIdPollIdAndPlayerExternalId(commandMessage.getPollVote().pollId(), commandMessage.getUserId())
                    .ifPresent(matchMakingService::unregisterMathPlayer);
            // TODO:  notify?
        }
    }

    @Override
    public Command getCommand() {
        return Command.VOTE;
    }
}
