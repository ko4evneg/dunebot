package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class VoteCommandProcessor extends CommandProcessor {
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final TaskScheduler taskScheduler;
    private final Clock clock;

    private static final int MATCH_START_DELAY = 60;
    private final Map<Long, ScheduledFuture<?>> scheduledTasksByMatchIds = new ConcurrentHashMap<>();

    @Override
    public void process(CommandMessage commandMessage) {
        List<Integer> selectedPollAnswers = commandMessage.getPollVote().selectedAnswerId();
        if (selectedPollAnswers.contains(SettingConstants.POSITIVE_POLL_OPTION_ID)) {
            registerMatchPlayer(commandMessage);
        } else {
            unregisterMatchPlayer(commandMessage);
        }
    }

    private void registerMatchPlayer(CommandMessage commandMessage) {
        playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player ->
                matchRepository.findByExternalPollIdPollId(commandMessage.getPollVote().pollId())
                        .ifPresent(match -> processPlayerRegistration(player, match)));
    }

    private void processPlayerRegistration(Player player, Match match) {
        int currentPositiveAnswersCount = match.getPositiveAnswersCount();
        int actualCount = currentPositiveAnswersCount + 1;
        match.setPositiveAnswersCount(actualCount);
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.save(matchPlayer);
        });
        if (actualCount == match.getModType().getPlayersCount()) {
            ScheduledFuture<?> existingScheduledTask = scheduledTasksByMatchIds.remove(match.getId());
            if (existingScheduledTask != null) {
                existingScheduledTask.cancel(false);
            }
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(() -> messagingService
                    .sendMessageAsync(getFullMatchMessage(match)).whenComplete((externalMessageDto, throwable) -> {
                        deleteExistingOldSubmitMessage(match);
                        match.setExternalSubmitId(new ExternalMessageId(externalMessageDto));
                        matchRepository.save(match);
                    }), Instant.now(clock).plusSeconds(MATCH_START_DELAY));
            scheduledTasksByMatchIds.put(match.getId(), scheduledTask);
        }
    }

    private MessageDto getFullMatchMessage(Match match) {
        String matchTopicChatId = match.getExternalPollId().getChatIdString();
        Integer replyTopicId = match.getExternalPollId().getReplyId();
        return new MessageDto(matchTopicChatId, "notify match is full", replyTopicId, null);
    }

    private void deleteExistingOldSubmitMessage(Match match) {
        if (match.getExternalSubmitId() != null) {
            Integer externalSubmitMessageId = match.getExternalSubmitId().getMessageId();
            Long externalSubmitChatId = match.getExternalSubmitId().getChatId();
            Integer submitReplyTopicId = match.getExternalSubmitId().getReplyId();
            messagingService.deleteMessageAsync(new ExternalMessageId(externalSubmitMessageId, externalSubmitChatId, submitReplyTopicId));
        }
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
