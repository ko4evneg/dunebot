package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.ConfirmMatchDto;
import ru.trainithard.dunebot.service.dto.MatchSubmitDto;
import ru.trainithard.dunebot.service.telegram.TelegramService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
    private final TelegramService telegramService;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    // TODO:
    private final PlayerRepository playerRepository;
    private final TransactionTemplate transactionTemplate;

    private static final List<String> POLL_OPTIONS = List.of("Да", "Нет", "Результат");

    @Override
    public void requestNewMatch(Player initiator, ModType modType) throws TelegramApiException {
        Match match = new Match();
        match.setModType(modType);
        MatchPlayer matchPlayer = new MatchPlayer();
        matchPlayer.setPlayer(initiator);

        SendPoll sendPoll = getNewPoll(initiator, modType);
        telegramService.sendPoll(sendPoll, ((message, throwable) -> {
            if (throwable == null) {
                transactionTemplate.executeWithoutResult(status -> {
                    String pollId = message.getPoll().getId();
                    match.setTelegramPollId(pollId);
                    int messageId = message.getMessageId();
                    match.setTelegramMessageId(messageId);
                    match.setOwner(initiator);
                    Match savedMatch = matchRepository.save(match);
                    matchPlayer.setMatch(savedMatch);
                    matchPlayerRepository.save(matchPlayer);
                });
            }
        }));
    }

    private SendPoll getNewPoll(Player initiator, ModType modType) {
        SendPoll sendPoll = new SendPoll();
        sendPoll.setQuestion("Игрок " + initiator.getFriendlyName() + " призывает всех на матч в " + modType.getModName());
        sendPoll.setAllowMultipleAnswers(false);
        sendPoll.setIsAnonymous(false);
        sendPoll.setChatId(SettingConstants.CHAT_ID);
        sendPoll.setMessageThreadId(getTopicId(modType));
        sendPoll.setOptions(POLL_OPTIONS);
        return sendPoll;
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> SettingConstants.TOPIC_ID_CLASSIC;
            case UPRISING_4, UPRISING_6 -> SettingConstants.TOPIC_ID_UPRISING;
        };
    }

    @Override
    public void cancelMatch(User user) throws TelegramApiException {
        Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(user.getId());
        if (latestOwnedMatchOptional.isPresent()) {
            Match match = latestOwnedMatchOptional.get();
            if (match.isFinished()) {
                // TODO:  notify
                return;
            }
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setMessageId(match.getTelegramMessageId());
            deleteMessage.setChatId(SettingConstants.CHAT_ID);
            telegramService.deleteMessage(deleteMessage, (bool, throwable) ->
                    transactionTemplate.executeWithoutResult(status -> {
                        matchRepository.delete(match);
                        matchPlayerRepository.deleteAll(match.getMatchPlayers());
                    })
            );
        }
    }

    @Override
    public void requestMatchSubmit(Player player) {

    }

    @Override
    public void acceptMatchSubmit(MatchSubmitDto matchSubmit) {

    }

    @Override
    public void confirmMatchSubmit(ConfirmMatchDto confirmMatch) {

    }
}
