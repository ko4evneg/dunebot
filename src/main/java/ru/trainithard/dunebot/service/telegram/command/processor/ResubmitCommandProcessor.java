package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.SubmitValidatedMatchRetriever;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResubmitCommandProcessor extends CommandProcessor {
    private static final String TIMEOUT_MATCH_FINISH_MESSAGE = "Матч %d завершен без результата, так как превышено максимальное количество попыток регистрации мест (%d)";

    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final SubmitCommandProcessor submitCommandProcessor;
    private final SubmitValidatedMatchRetriever validatedMatchRetriever;


    @Override
    public void process(CommandMessage commandMessage) {
        Match match = validatedMatchRetriever.getValidatedMatch(commandMessage);
        if (!match.isResubmitAllowed(SettingConstants.RESUBMITS_LIMIT)) {
            matchFinishingService.finishUnsuccessfullySubmittedMatch(match.getId(), String.format(TIMEOUT_MATCH_FINISH_MESSAGE, match.getId(), SettingConstants.RESUBMITS_LIMIT));
        }

        process(match);
    }

    void process(Match match) {
        List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
        updateSubmitsData(match);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(registeredMatchPlayers);
        });
        submitCommandProcessor.process(match);
    }

    private void updateSubmitsData(Match match) {
        match.setSubmitsRetryCount(match.getSubmitsRetryCount() + 1);
        match.setSubmitsCount(0);
        match.getMatchPlayers().forEach(matchPlayer -> {
            matchPlayer.setCandidatePlace(null);
            matchPlayer.setSubmitMessageId(null);
        });
    }

    @Override
    public Command getCommand() {
        return Command.RESUBMIT;
    }
}
