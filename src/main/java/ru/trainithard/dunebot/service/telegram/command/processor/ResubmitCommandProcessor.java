package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.SubmitValidatedMatchRetriever;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

/**
 * Resets current results and initiates match results requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResubmitCommandProcessor extends CommandProcessor {
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MatchFinishingService matchFinishingService;
    private final SubmitCommandProcessor submitCommandProcessor;
    private final SubmitValidatedMatchRetriever validatedMatchRetriever;
    private final SettingsService settingsService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        log.debug("{}: resubmit started", loggingId);

        Match match = validatedMatchRetriever.getValidatedResubmitMatch(commandMessage);
        int resubmitsLimit = settingsService.getIntSetting(SettingKey.RESUBMITS_LIMIT);
        if (!match.isResubmitAllowed(resubmitsLimit)) {
            ExternalMessage timeoutFinishMessage = getTimeoutFinishMessage(match.getId(), resubmitsLimit);
            matchFinishingService.finishNotSubmittedMatch(match.getId(), timeoutFinishMessage, loggingId);
        }

        process(match, loggingId);

        log.debug("{}: resubmit ended", loggingId);
    }

    private ExternalMessage getTimeoutFinishMessage(Long matchId, int resubmitsLimit) {
        return new ExternalMessage()
                .startBold().append("Матч ").append(matchId).endBold()
                .append(" завершен без результата, так как превышено максимальное количество попыток регистрации мест (")
                .append(resubmitsLimit).append(")");
    }

    void process(Match match, int loggingId) {
        log.debug("{}: resubmit processing started", loggingId);

        List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
        updateSubmitsData(match);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(registeredMatchPlayers);
        });
        submitCommandProcessor.process(match, loggingId);

        log.debug("{}: resubmit processing ended", loggingId);
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
