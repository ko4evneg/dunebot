package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.telegram.command.CallbackSymbol;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.validator.SubmitMatchValidator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResubmitCallbackProcessor extends AbstractSubmitCommandProcessor {
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final SubmitMatchValidator submitMatchValidator;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: RESUBMIT_CALLBACK started", logId());

        String[] callbackData = commandMessage.getCallback().split(CallbackSymbol.RESUBMIT_CALLBACK_SYMBOL.getSymbol());
        long matchId = Long.parseLong(callbackData[0]);
        long externalResubmitterId = Long.parseLong(callbackData[1]);

        Match match = matchRepository.findWithMatchPlayersBy(matchId).orElseThrow();
        submitMatchValidator.validateReSubmitMatch(commandMessage, match);
        log.debug("{}: match {} found and validated", logId(), matchId);

        Player resubmitter = playerRepository.findByExternalId(externalResubmitterId).orElseThrow();
        prepareAndSaveMatchAndPlayersForResubmit(match, resubmitter);
        messagingService.deleteMessageAsync(match.getExternalSubmitId());

        sendSubmitMessages(match, externalResubmitterId);
        rescheduleSubmitTasks(match.getId());
        log.debug("{}: RESUBMIT_CALLBACK ended", logId());
    }

    private void prepareAndSaveMatchAndPlayersForResubmit(Match match, Player submitter) {
        match.prepareForResubmit(submitter);
        match.getMatchPlayers().forEach(MatchPlayer::resetSubmitData);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.saveAll(match.getMatchPlayers());
        });
        log.debug("{}: match {} and its players prepared for resubmit and receive ON_SUBMIT state and saved", logId(), match.getId());
    }

    @Override
    public Command getCommand() {
        return Command.RESUBMIT_CALLBACK;
    }
}
