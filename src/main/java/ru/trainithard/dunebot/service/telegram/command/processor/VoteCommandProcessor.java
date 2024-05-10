package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskScheduler;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Accepts the vote in a match poll from external messaging system.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteCommandProcessor extends CommandProcessor {
    private static final int POSITIVE_POLL_OPTION_ID = 0;

    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final DuneBotTaskScheduler taskScheduler;
    private final SettingsService settingsService;
    private final ExternalMessageFactory externalMessageFactory;
    private final Clock clock;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: VOTE started", logId());

        List<Integer> selectedPollAnswers = commandMessage.getPollVote().selectedAnswerId();
        log.debug("{}: option: {}", logId(), selectedPollAnswers);
        if (selectedPollAnswers.contains(POSITIVE_POLL_OPTION_ID)) {
            registerMatchPlayerVote(commandMessage);
        } else {
            unregisterMatchPlayerVote(commandMessage);
        }

        log.debug("{}: VOTE ended", logId());
    }

    private void registerMatchPlayerVote(CommandMessage commandMessage) {
        matchRepository.findByExternalPollIdPollId(commandMessage.getPollVote().pollId())
                .ifPresentOrElse(match -> {
                            log.debug("{}: match {} found", logId(), match.getId());
                            Optional<Player> playerOptional = playerRepository.findByExternalId(commandMessage.getUserId());
                            Player player;
                            if (playerOptional.isEmpty()) {
                                log.debug("{}: player {} not found. Creating guest...", logId(), commandMessage.getUserId());
                                int nextGuestIndex = settingsService.getIntSetting(SettingKey.NEXT_GUEST_INDEX);
                                Player guestPlayer = Player.createGuestPlayer(commandMessage, nextGuestIndex);
                                settingsService.saveSetting(SettingKey.NEXT_GUEST_INDEX, Integer.toString(nextGuestIndex + 1));
                                player = playerRepository.save(guestPlayer);
                                log.debug("{}: player {} ({}) saved as guest", logId(), player.getId(), commandMessage.getUserId());
                            } else {
                                player = playerOptional.get();
                                log.debug("{}: player {} found", logId(), player.getId());
                            }
                            if (player.isGuest()) {
                                ExternalMessage guestVoteMessage = externalMessageFactory.getGuestMessageDto(player);
                                MessageDto messageDto = new MessageDto(player.getExternalChatId(), guestVoteMessage, null, null);
                                messagingService.sendMessageAsync(messageDto).exceptionally(processException(player, logId()));
                            }
                            processPlayerVoteRegistration(player, match);
                        },
                        () -> log.debug("{}: match not found", logId())
                );
    }

    private Function<Throwable, ExternalMessageDto> processException(Player player, int logId) {
        return exception -> {
            if (exception instanceof TelegramApiRequestException telegramException && telegramException.getErrorCode().equals(403)) {
                player.setChatBlocked(true);
                playerRepository.save(player);
                log.debug("{}: player {} ({}) received chat_block flag", logId, player.getId(), player.getExternalChatId());
            }
            return null;
        };
    }

    private void processPlayerVoteRegistration(Player player, Match match) {
        log.debug("{}: match {} (positiveAnswers: {}) player {} vote registration...",
                logId(), match.getId(), match.getPositiveAnswersCount(), player.getId());

        int updatedPositiveAnswersCount = match.getPositiveAnswersCount() + 1;
        match.setPositiveAnswersCount(updatedPositiveAnswersCount);
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        Match savedMatch = transactionTemplate.execute(status -> {
            matchPlayerRepository.save(matchPlayer);
            return matchRepository.save(match);
        });
        log.debug("{}: match {} player {} vote saved, positiveAnswers: {}",
                logId(), match.getId(), player.getId(), savedMatch == null ? "null" : savedMatch.getPositiveAnswersCount());

        if (match.isReadyToStart()) {
            rescheduleNewMatchStart(match.getId());
            if (match.getModType() == ModType.UPRISING_6) {
                match.setState(MatchState.FAILED);
                matchRepository.save(match);
            }
        }
    }

    private void rescheduleNewMatchStart(long matchId) {
        int matchStartDelay = settingsService.getIntSetting(SettingKey.MATCH_START_DELAY);
        Instant matchStartInstant = Instant.now(clock).plusSeconds(matchStartDelay);
        taskScheduler.reschedule(() -> {
                    Optional<Match> freshMatch = matchRepository.findWithMatchPlayersBy(matchId);
                    if (freshMatch.isEmpty()) {
                        log.debug("0: match {} start not found the match", matchId);
                        return;
                    }
                    messagingService.sendMessageAsync(getMatchStartMessage(freshMatch.get()))
                            .whenComplete((externalMessageDto, throwable) -> {
                                log.debug("0: match {} start callback received. Current time: {}",
                                        matchId, Instant.now(clock));
                                if (throwable != null) {
                                    log.error("0: match {} start callback failure...", matchId, throwable);
                                }
                                matchRepository.findById(matchId).ifPresent(cbMatch -> {
                                    deleteExistingOldSubmitMessage(cbMatch);
                                    cbMatch.setExternalStartId(new ExternalMessageId(externalMessageDto));
                                    matchRepository.save(cbMatch);
                                });
                            });
                },
                new DuneTaskId(DuneTaskType.START_MESSAGE, matchId), matchStartInstant);
    }

    private MessageDto getMatchStartMessage(Match match) {
        List<String> regularPlayerMentions = new ArrayList<>();
        List<String> guestPlayerMentions = new ArrayList<>();
        List<String> blockedChatMentions = new ArrayList<>();
        for (MatchPlayer matchPlayer : matchPlayerRepository.findByMatch(match)) {
            Player player = matchPlayer.getPlayer();
            String mention = MarkdownEscaper.getEscapedMention(player.getMentionTag(), player.getExternalId());
            log.debug("{}: match {} start message building... player {} (guest: {}, chat_blocked: {})",
                    logId(), match.getId(), player.getId(), player.isGuest(), player.isChatBlocked());
            if (player.isChatBlocked()) {
                blockedChatMentions.add(mention);
            } else if (player.isGuest()) {
                guestPlayerMentions.add(mention);
            } else {
                regularPlayerMentions.add(mention);
            }
        }
        String matchTopicChatId = match.getExternalPollId().getChatIdString();
        Integer topicId = match.getExternalPollId().getReplyId();
        Integer replyMessageId = match.getExternalPollId().getMessageId();
        ExternalMessage startMessage = externalMessageFactory
                .getStartMessage(match, regularPlayerMentions, guestPlayerMentions, blockedChatMentions);
        return new MessageDto(matchTopicChatId, startMessage, topicId, replyMessageId, null);
    }

    private void deleteExistingOldSubmitMessage(Match match) {
        if (match.getExternalStartId() != null) {
            Integer externalMessageId = match.getExternalStartId().getMessageId();
            Long externalChatId = match.getExternalStartId().getChatId();
            Integer externalMessageReplyId = match.getExternalStartId().getReplyId();
            messagingService.deleteMessageAsync(new ExternalMessageId(externalMessageId, externalChatId, externalMessageReplyId));
        }
    }

    private void unregisterMatchPlayerVote(CommandMessage commandMessage) {
        matchPlayerRepository
                .findByMatchExternalPollIdPollIdAndPlayerExternalId(commandMessage.getPollVote().pollId(), commandMessage.getUserId())
                .ifPresentOrElse(matchPlayer -> {
                            Match match = matchPlayer.getMatch();
                            log.debug("{}: match {} (positiveAnswers: {}) player {} found",
                                    logId(), match.getId(), match.getPositiveAnswersCount(), matchPlayer.getPlayer().getId());

                            if (match.getState() == MatchState.NEW) {
                                int currentPositiveAnswersCount = match.getPositiveAnswersCount();
                                match.setPositiveAnswersCount(currentPositiveAnswersCount - 1);
                                Match savedMatch = transactionTemplate.execute(status -> {
                                    matchPlayerRepository.delete(matchPlayer);
                                    return matchRepository.save(match);
                                });
                                log.debug("{}: updated match {} player {} vote revoked. positiveAnswers: {}",
                                        logId(), match.getId(), matchPlayer.getPlayer().getId(),
                                        savedMatch == null ? "null" : savedMatch.getPositiveAnswersCount());

                                if (match.hasMissingPlayers()) {
                                    taskScheduler.cancel(new DuneTaskId(DuneTaskType.START_MESSAGE, match.getId()));
                                    ExternalMessageId externalStartId = match.getExternalStartId();
                                    if (externalStartId != null) {
                                        messagingService.deleteMessageAsync(externalStartId);
                                    }
                                }
                            }
                        },
                        () -> log.debug("{}: matchPlayer for player {} not found", logId(), commandMessage.getUserId())
                );
    }

    @Override
    public Command getCommand() {
        return Command.VOTE;
    }
}
