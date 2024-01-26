package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CancelCommandProcessor extends CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CancelCommandProcessor.class);
    private static final String FINISHED_MATCH_EXCEPTION_MESSAGE = "Запрещено отменять завершенные матчи!";
    private static final Set<MatchState> finishedMatchStates = EnumSet.of(MatchState.FAILED, MatchState.FINISHED);

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: cancel started", loggingId);

        playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player -> {
            Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(player.getId());
            if (latestOwnedMatchOptional.isPresent()) {
                Match latestOwnedMatch = latestOwnedMatchOptional.get();
                logger.debug("{}: to-cancel match found, id {}", loggingId, latestOwnedMatch.getId());

                if (finishedMatchStates.contains(latestOwnedMatch.getState())) {
                    throw new AnswerableDuneBotException(FINISHED_MATCH_EXCEPTION_MESSAGE, player.getExternalChatId());
                }

                messagingService.deleteMessageAsync(latestOwnedMatch.getExternalPollId());
                transactionTemplate.executeWithoutResult(status -> {
                    matchRepository.delete(latestOwnedMatch);
                    matchPlayerRepository.deleteAll(latestOwnedMatch.getMatchPlayers());
                });
            }
        });

        logger.debug("{}: cancel ended", loggingId);
    }

    @Override
    public Command getCommand() {
        return Command.CANCEL;
    }
}
