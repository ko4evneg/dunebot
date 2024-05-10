package ru.trainithard.dunebot.service.telegram.command.task;

import lombok.extern.slf4j.Slf4j;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.messaging.ExternalMessageId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class StartMatchTask implements Runnable {
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MessagingService messagingService;
    private final ExternalMessageFactory messageFactory;
    private final Clock clock;
    private final long matchId;

    public StartMatchTask(MatchRepository matchRepository, MatchPlayerRepository matchPlayerRepository,
                          MessagingService messagingService, ExternalMessageFactory messageFactory, Clock clock, long matchId) {
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.messagingService = messagingService;
        this.messageFactory = messageFactory;
        this.clock = clock;
        this.matchId = matchId;
    }

    @Override
    public void run() {
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
    }

    private void deleteExistingOldSubmitMessage(Match match) {
        if (match.getExternalStartId() != null) {
            Integer externalMessageId = match.getExternalStartId().getMessageId();
            Long externalChatId = match.getExternalStartId().getChatId();
            Integer externalMessageReplyId = match.getExternalStartId().getReplyId();
            messagingService.deleteMessageAsync(new ExternalMessageId(externalMessageId, externalChatId, externalMessageReplyId));
        }
    }

    private MessageDto getMatchStartMessage(Match match) {
        List<String> regularPlayerMentions = new ArrayList<>();
        List<String> guestPlayerMentions = new ArrayList<>();
        List<String> blockedChatMentions = new ArrayList<>();
        for (MatchPlayer matchPlayer : matchPlayerRepository.findByMatch(match)) {
            Player player = matchPlayer.getPlayer();
            String mention = MarkdownEscaper.getEscapedMention(player.getMentionTag(), player.getExternalId());
            log.debug("0: match {} start message building... player {} (guest: {}, chat_blocked: {})",
                    match.getId(), player.getId(), player.isGuest(), player.isChatBlocked());
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
        ExternalMessage startMessage = messageFactory
                .getStartMessage(match, regularPlayerMentions, guestPlayerMentions, blockedChatMentions);
        return new MessageDto(matchTopicChatId, startMessage, topicId, replyMessageId, null);
    }
}
