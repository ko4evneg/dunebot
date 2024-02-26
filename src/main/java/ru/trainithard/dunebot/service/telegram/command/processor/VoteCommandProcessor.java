package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static ru.trainithard.dunebot.configuration.SettingConstants.EXTERNAL_LINE_SEPARATOR;

@Service
@RequiredArgsConstructor
public class VoteCommandProcessor extends CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(VoteCommandProcessor.class);
    private static final int POSITIVE_POLL_OPTION_ID = 0;

    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final TaskScheduler dunebotTaskScheduler;
    private final SettingsService settingsService;
    private final Clock clock;

    private final Map<Long, ScheduledFuture<?>> scheduledTasksByMatchIds = new ConcurrentHashMap<>();

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: vote started", loggingId);

        List<Integer> selectedPollAnswers = commandMessage.getPollVote().selectedAnswerId();
        if (selectedPollAnswers.contains(POSITIVE_POLL_OPTION_ID)) {
            registerMatchPlayerVote(commandMessage, loggingId);
        } else {
            unregisterMatchPlayerVote(commandMessage, loggingId);
        }

        logger.debug("{}: vote ended", loggingId);
    }

    private void registerMatchPlayerVote(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: vote registration started", loggingId);
        matchRepository.findByExternalPollIdPollId(commandMessage.getPollVote().pollId())
                .ifPresent(match -> {
                            Optional<Player> playerOptional = playerRepository.findByExternalId(commandMessage.getUserId());
                            Player player;
                            if (playerOptional.isEmpty()) {
                                int nextGuestIndex = playerRepository.findNextGuestIndex();
                                Player guestPlayer = Player.createGuestPlayer(commandMessage, nextGuestIndex);
                                player = playerRepository.save(guestPlayer);
                            } else {
                                player = playerOptional.get();
                            }
                            processPlayerVoteRegistration(player, match, loggingId);
                        }
                );
    }

    private void processPlayerVoteRegistration(Player player, Match match, int loggingId) {
        logger.debug("{}: vote registration found player id: {}, match id: {}", loggingId, player.getId(), match.getId());

        int updatedPositiveAnswersCount = match.getPositiveAnswersCount() + 1;
        match.setPositiveAnswersCount(updatedPositiveAnswersCount);
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.save(matchPlayer);
        });

        if (match.isFull()) {
            cancelScheduledMatchStart(match);
            logger.debug("{}: vote match start message unscheduled", loggingId);

            // TODO:  logic for guest players start
            scheduleNewMatchStart(match);
            logger.debug("{}: vote match start message scheduled", loggingId);
        }
    }

    private void cancelScheduledMatchStart(Match match) {
        ScheduledFuture<?> existingScheduledTask = scheduledTasksByMatchIds.remove(match.getId());
        if (existingScheduledTask != null) {
            existingScheduledTask.cancel(false);
        }
    }

    private void scheduleNewMatchStart(Match match) {
        int matchStartDelay = settingsService.getIntSetting(SettingsService.MATCH_START_DELAY_KEY);
        Instant matchStartInstant = Instant.now(clock).plusSeconds(matchStartDelay);
        ScheduledFuture<?> scheduledTask =
                dunebotTaskScheduler.schedule(() -> messagingService.sendMessageAsync(getMatchStartMessage(match))
                                .whenComplete((externalMessageDto, throwable) -> {
                                    deleteExistingOldSubmitMessage(match);
                                    match.setExternalStartId(new ExternalMessageId(externalMessageDto));
                                    matchRepository.save(match);
                                }),
                        matchStartInstant);

        scheduledTasksByMatchIds.put(match.getId(), scheduledTask);
    }

    private MessageDto getMatchStartMessage(Match match) {
        List<String> regularPlayerMentions = new ArrayList<>();
        List<String> guestPlayerMentions = new ArrayList<>();
        for (MatchPlayer matchPlayer : matchPlayerRepository.findByMatch(match)) {
            Player player = matchPlayer.getPlayer();
            String mention = player.getMention();
            if (player.isGuest()) {
                guestPlayerMentions.add(mention);
            } else {
                regularPlayerMentions.add(mention);
            }
        }

        String matchTopicChatId = match.getExternalPollId().getChatIdString();
        Integer replyTopicId = match.getExternalPollId().getReplyId();
        StringBuilder messageText = new StringBuilder("*Матч ").append(match.getId()).append("* собран. Участники:")
                .append(EXTERNAL_LINE_SEPARATOR).append(String.join(", ", regularPlayerMentions));
        if (!guestPlayerMentions.isEmpty()) {
            messageText.append(EXTERNAL_LINE_SEPARATOR).append(EXTERNAL_LINE_SEPARATOR)
                    .append("*Внимание:* в матче есть незарегистрированные игроки. Они автоматически зарегистрированы " +
                            "под именем Vasya Pupkin и смогут подтвердить результаты матчей для регистрации результатов:")
                    .append(EXTERNAL_LINE_SEPARATOR).append(String.join(", ", guestPlayerMentions));
        }
        return new MessageDto(matchTopicChatId, MarkdownEscaper.getEscaped(messageText.toString()), replyTopicId, null);
    }

    private void deleteExistingOldSubmitMessage(Match match) {
        if (match.getExternalStartId() != null) {
            Integer externalStartMessageId = match.getExternalStartId().getMessageId();
            Long externalStartMessageChatId = match.getExternalStartId().getChatId();
            Integer startMessageReplyId = match.getExternalStartId().getReplyId();
            messagingService.deleteMessageAsync(new ExternalMessageId(externalStartMessageId, externalStartMessageChatId, startMessageReplyId));
        }
    }

    private void unregisterMatchPlayerVote(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: vote unregister started", loggingId);

        matchPlayerRepository
                .findByMatchExternalPollIdPollIdAndPlayerExternalId(commandMessage.getPollVote().pollId(), commandMessage.getUserId())
                .ifPresent(matchPlayer -> {
                    Match match = matchPlayer.getMatch();

                    logger.debug("{}: vote unregister found match id: {}, player id: {}", loggingId, match.getId(), matchPlayer.getPlayer().getId());
                    if (match.getState() == MatchState.NEW) {
                        int currentPositiveAnswersCount = match.getPositiveAnswersCount();
                        match.setPositiveAnswersCount(currentPositiveAnswersCount - 1);
                        transactionTemplate.executeWithoutResult(status -> {
                            matchRepository.save(match);
                            matchPlayerRepository.delete(matchPlayer);
                        });
                        if (match.hasMissingPlayers()) {
                            removeScheduledMatchStart(match);
                            ExternalMessageId externalStartId = match.getExternalStartId();
                            if (externalStartId != null) {
                                messagingService.deleteMessageAsync(externalStartId);
                            }
                        }
                    }
                });
    }

    private void removeScheduledMatchStart(Match match) {
        ScheduledFuture<?> oldScheduledTask = scheduledTasksByMatchIds.remove(match.getId());
        if (oldScheduledTask != null) {
            oldScheduledTask.cancel(false);
        }
    }

    @Override
    public Command getCommand() {
        return Command.VOTE;
    }
}
