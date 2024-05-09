package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderCommandProcessor extends CommandProcessor {
    private static final String SUCCESS_LEADER_REGISTRATION_MESSAGE_TEMPLATE =
            "Лидер для матча %d зарегистрирован. Ожидайте регистрации мест других игроков.";
    private final MatchPlayerRepository matchPlayerRepository;
    private final LeaderRepository leaderRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: LEADER started", logId());

        Callback callback = new Callback(commandMessage.getCallback());
        long matchPlayerId = callback.matchPlayerId;
        matchPlayerRepository.findById(matchPlayerId).ifPresent(matchPlayer -> {
            long leaderId = callback.leaderId;
            log.debug("{}: callback parsed. MatchPlayer {}, leader {}", logId(), matchPlayerId, leaderId);
            Leader leader = leaderRepository.findById(leaderId).orElseThrow();
            log.debug("{}: leader {} found", logId(), leader.getName());
            matchPlayer.setLeader(leader);
            matchPlayerRepository.save(matchPlayer);
            log.debug("{}: leader saved to match", logId());

            long playerChatId = matchPlayer.getPlayer().getExternalChatId();
            String message = String.format(SUCCESS_LEADER_REGISTRATION_MESSAGE_TEMPLATE, matchPlayer.getMatch().getId());
            MessageDto messageDto =
                    new MessageDto(playerChatId, new ExternalMessage(message), null, null);
            messagingService.sendMessageAsync(messageDto);
        });

        log.debug("{}: LEADER ended", logId());
    }

    @Override
    public Command getCommand() {
        return Command.LEADER;
    }

    private static class Callback {
        private final long matchPlayerId;
        private final long leaderId;

        private Callback(String callbackText) {
            String[] callbackData = callbackText.split("_L_");
            this.matchPlayerId = Long.parseLong(callbackData[0]);
            this.leaderId = Long.parseLong(callbackData[1]);
        }
    }
}
