package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.ScreenshotFileIOException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.SubmitValidatedMatchRetriever;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public void process(CommandMessage commandMessage) {
        log.debug("{}: RESUBMIT started", logId());
        Match match = validatedMatchRetriever.getValidatedResubmitMatch(commandMessage);
        process(match);
        log.debug("{}: RESUBMIT ended", logId());
    }

    void process(Match match) {
        log.debug("{}: RESUBMIT(internal) started", logId());

        int resubmitsLimit = settingsService.getIntSetting(SettingKey.RESUBMITS_LIMIT);
        log.debug("{}: match {} resubmit limit {}", logId(), match.getId(), resubmitsLimit);
        if (!match.isResubmitAllowed(resubmitsLimit)) {
            log.debug("{}: resubmit is not allowed. Finishing by resubmits llimit reached...", logId());
            matchFinishingService.finishNotSubmittedMatch(match.getId(), true);
        } else {
            List<MatchPlayer> registeredMatchPlayers = match.getMatchPlayers();
            log.debug("{}: matchPlayers retrieved", logId());
            resetMatchData(match);
            transactionTemplate.executeWithoutResult(status -> {
                matchRepository.save(match);
                matchPlayerRepository.saveAll(registeredMatchPlayers);
                log.debug("{}: match {} and matchPlayers submit discarded info and updated limits saved.", logId(), match.getId());
            });
            submitCommandProcessor.process(match);
        }

        log.debug("{}: RESUBMIT(internal) ended", logId());
    }

    private void resetMatchData(Match match) {
        try {
            String screenshotPath = match.getScreenshotPath();
            if (screenshotPath != null) {
                Files.deleteIfExists(Path.of(screenshotPath));
                match.setScreenshotPath(null);
            }
            match.prepareForResubmit();
            match.getMatchPlayers().forEach(matchPlayer -> {
                matchPlayer.setCandidatePlace(null);
                deleteOldSubmitMessage(matchPlayer.getSubmitMessageId());
                matchPlayer.setSubmitMessageId(null);
                matchPlayer.setLeader(null);
            });
        } catch (IOException e) {
            throw new ScreenshotFileIOException("Can not remove old screenshot file");
        }
    }

    private void deleteOldSubmitMessage(ExternalMessageId submitMessageExternalId) {
        if (submitMessageExternalId != null) {
            messagingService.deleteMessageAsync(submitMessageExternalId);
        }
    }

    @Override
    public Command getCommand() {
        return Command.RESUBMIT;
    }
}
