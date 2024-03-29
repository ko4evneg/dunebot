package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

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
    private final MessagingService messagingService;
    private final TaskScheduler dunebotTaskScheduler;
    private final SettingsService settingsService;
    private final Clock clock;

    private final Map<Long, ScheduledFuture<?>> scheduledTasksByMatchIds = new ConcurrentHashMap<>();

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: VOTE started", logId());

        List<Integer> selectedPollAnswers = commandMessage.getPollVote().selectedAnswerId();
        log.debug("{}: poll options: {}", logId(), selectedPollAnswers);
        if (selectedPollAnswers.contains(POSITIVE_POLL_OPTION_ID)) {
            registerMatchPlayerVote(commandMessage);
        } else {
            unregisterMatchPlayerVote(commandMessage);
        }

        log.debug("{}: VOTE ended", logId());
    }

    private void registerMatchPlayerVote(CommandMessage commandMessage) {
        log.debug("{}: vote registration...", logId());
        matchRepository.findByExternalPollIdPollId(commandMessage.getPollVote().pollId())
                .ifPresent(match -> {
                    log.debug("{}: found match {}", logId(), match.getId());
                            Optional<Player> playerOptional = playerRepository.findByExternalId(commandMessage.getUserId());
                            Player player;
                            if (playerOptional.isEmpty()) {
                                log.debug("{}: player telegram id {} not found", logId(), commandMessage.getUserId());
                                int nextGuestIndex = playerRepository.findNextGuestIndex();
                                Player guestPlayer = Player.createGuestPlayer(commandMessage, nextGuestIndex);
                                player = playerRepository.save(guestPlayer);
                                log.debug("{}: new guest player saved", logId(), commandMessage.getUserId());
                            } else {
                                player = playerOptional.get();
                                log.debug("{}: player telegram id {} found", logId(), commandMessage.getUserId());
                            }
                    if (player.isGuest()) {
                        messagingService.sendMessageAsync(getGuestMessageDto(player));
                    }
                    processPlayerVoteRegistration(player, match);
                        }
                );
        log.debug("{}: vote registered", logId());
    }

    private MessageDto getGuestMessageDto(Player player) {
        ExternalMessage guestVoteMessage = new ExternalMessage("""
                Вас приветствует DuneBot! Вы ответили да в опросе по рейтинговой игре - это значит, что по завершении \
                игры вам придет опрос, где нужно будет указать занятое в игре место (и загрузить скриншот матча в \
                случае победы) - не волнуйтесь, бот подскажет что делать.""").newLine()
                .append("Также вы автоматически зарегистрированы у бота как гость под именем ")
                .append(player.getFirstName()).append(" (").append(player.getSteamName()).append(") ").append(player.getLastName())
                .append(" - это значит, что вы не можете выполнять некоторые команды бота и не будете включены " +
                        "в результаты рейтинга.").newLine()
                .append("Для того, чтобы подтвердить регистрацию, выполните в этом чате команду")
                .appendBold(" '/refresh_profile Имя (Steam) Фамилия'").append(".").newLine()
                .appendBold("Желательно это  сделать прямо сейчас.").newLine()
                .append("Подробная информация о боте: /help.");
        return new MessageDto(player.getExternalChatId(), guestVoteMessage, null, null);
    }

    private void processPlayerVoteRegistration(Player player, Match match) {
        log.debug("{}: player id: {}, match id: {} vote registration...", logId(), player.getId(), match.getId());

        int updatedPositiveAnswersCount = match.getPositiveAnswersCount() + 1;
        match.setPositiveAnswersCount(updatedPositiveAnswersCount);
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        transactionTemplate.executeWithoutResult(status -> {
            matchRepository.save(match);
            matchPlayerRepository.save(matchPlayer);
            log.debug("{}: match {} and matchPlayer {} saved, positiveAnswers: {}",
                    logId(), player.getId(), match.getId(), updatedPositiveAnswersCount);
        });

        if (match.isFull()) {
            cancelScheduledMatchStart(match);
            scheduleNewMatchStart(match);
        }
    }

    private void cancelScheduledMatchStart(Match match) {
        ScheduledFuture<?> existingScheduledTask = scheduledTasksByMatchIds.remove(match.getId());
        if (existingScheduledTask != null) {
            existingScheduledTask.cancel(false);
        }
        log.debug("{}: vote match start message unscheduled", logId());
    }

    private void scheduleNewMatchStart(Match match) {
        int matchStartDelay = settingsService.getIntSetting(SettingKey.MATCH_START_DELAY);
        Instant matchStartInstant = Instant.now(clock).plusSeconds(matchStartDelay);
        ScheduledFuture<?> scheduledTask =
                dunebotTaskScheduler.schedule(() -> messagingService.sendMessageAsync(getMatchStartMessage(match))
                                .whenComplete((externalMessageDto, throwable) -> {
                                    deleteExistingOldSubmitMessage(match);
                                    match.setExternalStartId(new ExternalMessageId(externalMessageDto));
                                    matchRepository.save(match);
                                    log.debug("{}: match externalStart updated to '{}' and saved", logId(), matchStartInstant);
                                }),
                        matchStartInstant);

        scheduledTasksByMatchIds.put(match.getId(), scheduledTask);
        log.debug("{}: vote match start message scheduled to '{}'", logId(), matchStartInstant);
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
        Integer topicId = match.getExternalPollId().getReplyId();
        Integer replyMessageId = match.getExternalPollId().getMessageId();
        ExternalMessage startMessage = new ExternalMessage()
                .startBold().append("Матч ").append(match.getId()).endBold().append(" собран. Участники:")
                .newLine().append(String.join(", ", regularPlayerMentions));
        if (!guestPlayerMentions.isEmpty()) {
            startMessage.newLine().newLine()
                    .appendBold("Внимание:")
                    .append(" в матче есть незарегистрированные игроки. Они автоматически зарегистрированы " +
                            "под именем Vasya Pupkin и смогут подтвердить результаты матчей для регистрации результатов:")
                    .newLine().append(String.join(", ", guestPlayerMentions));
        }
        return new MessageDto(matchTopicChatId, startMessage, topicId, replyMessageId, null);
    }

    private void deleteExistingOldSubmitMessage(Match match) {
        if (match.getExternalStartId() != null) {
            Integer externalStartMessageId = match.getExternalStartId().getMessageId();
            Long externalStartMessageChatId = match.getExternalStartId().getChatId();
            Integer startMessageReplyId = match.getExternalStartId().getReplyId();
            messagingService.deleteMessageAsync(new ExternalMessageId(externalStartMessageId, externalStartMessageChatId, startMessageReplyId));
        }
    }

    private void unregisterMatchPlayerVote(CommandMessage commandMessage) {
        log.debug("{}: vote revocation...", logId());

        matchPlayerRepository
                .findByMatchExternalPollIdPollIdAndPlayerExternalId(commandMessage.getPollVote().pollId(), commandMessage.getUserId())
                .ifPresent(matchPlayer -> {
                    log.debug("{}: matchPlayer {} found", logId(), matchPlayer.getId());
                    Match match = matchPlayer.getMatch();

                    if (match.getState() == MatchState.NEW) {
                        int currentPositiveAnswersCount = match.getPositiveAnswersCount();
                        match.setPositiveAnswersCount(currentPositiveAnswersCount - 1);
                        transactionTemplate.executeWithoutResult(status -> {
                            matchRepository.save(match);
                            matchPlayerRepository.delete(matchPlayer);
                            log.debug("{}: updated match {} saved. matchPlayer {} deleted", logId(), match.getId(), matchPlayer.getId());
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
        log.debug("{}: match {} start unscheduled", logId(), match.getId());
    }

    @Override
    public Command getCommand() {
        return Command.VOTE;
    }
}
