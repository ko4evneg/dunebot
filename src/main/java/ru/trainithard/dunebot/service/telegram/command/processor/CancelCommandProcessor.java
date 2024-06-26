package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Optional;

/**
 * Cancels existing not finished match owned by requester.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelCommandProcessor extends CommandProcessor {
    private static final String FINISHED_MATCH_EXCEPTION_MESSAGE = "Запрещено отменять завершенные матчи!";

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: CANCEL started", logId());

        playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player -> {
            log.debug("{}: player {} found", logId(), player.getId());
            Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatchWithMatchPlayersBy(player.getId());
            if (latestOwnedMatchOptional.isPresent()) {
                Match latestOwnedMatch = latestOwnedMatchOptional.get();
                log.debug("{}: to-cancel match {} found", logId(), latestOwnedMatch.getId());
                //TODO: restrict onsubmit cancel
                if (MatchState.getEndedMatchStates().contains(latestOwnedMatch.getState())) {
                    throw new AnswerableDuneBotException(FINISHED_MATCH_EXCEPTION_MESSAGE, player.getExternalChatId());
                }
                messagingService.deleteMessageAsync(latestOwnedMatch.getExternalPollId());
                transactionTemplate.executeWithoutResult(status -> {
                    latestOwnedMatch.setState(MatchState.CANCELLED);
                    matchRepository.save(latestOwnedMatch);
                    log.debug("{}: match and matchPlayers deleted", logId());
                });
            } else {
                log.debug("{}: no matches found to cancel, sending message...", logId());
                MessageDto messageDto = new MessageDto(commandMessage,
                        new ExternalMessage("Не найдены матчи, которые можно завершить!"), null);
                messagingService.sendMessageAsync(messageDto);
            }
        });

        log.debug("{}: CANCEL ended", logId());
    }

    @Override
    public Command getCommand() {
        return Command.CANCEL;
    }
}
