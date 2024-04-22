package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Leader;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.LeaderRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderCommandProcessor extends CommandProcessor {
    private static final String SUCCESS_LEADER_REGISTRATION_MESSAGE = "Лидер зарегистрирован. Ожидайте регистрации мест других игроков.";
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final LeaderRepository leaderRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: LEADER started", logId());

        Callback callback = new Callback(commandMessage.getCallback());
        long matchId = callback.matchId;
        long leaderId = callback.leaderId;
        log.debug("{}: callback parsed. Match id: {}, leader id: {}", logId(), matchId, leaderId);
        Leader leader = leaderRepository.findById(leaderId).orElseThrow();
        log.debug("{}: leader {} found", logId(), leader.getName());
        matchRepository.saveLeader(matchId, leader);
        log.debug("{}: leader saved to match", logId());

        Player answeredPlayer = playerRepository.findByExternalId(commandMessage.getUserId()).orElseThrow();
        MessageDto messageDto =
                new MessageDto(answeredPlayer.getExternalChatId(), new ExternalMessage(SUCCESS_LEADER_REGISTRATION_MESSAGE), null, null);
        messagingService.sendMessageAsync(messageDto);

        log.debug("{}: LEADER ended", logId());
    }

    @Override
    public Command getCommand() {
        return Command.LEADER;
    }

    private static class Callback {
        private final long matchId;
        private final long leaderId;

        private Callback(String callbackText) {
            String[] callbackData = callbackText.split("_L_");
            this.matchId = Long.parseLong(callbackData[0]);
            this.leaderId = Long.parseLong(callbackData[1]);
        }
    }
}
