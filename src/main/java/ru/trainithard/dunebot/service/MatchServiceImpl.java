package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.PlayerMatch;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerMatchRepository;
import ru.trainithard.dunebot.service.dto.ConfirmMatchDto;
import ru.trainithard.dunebot.service.dto.MatchSubmitDto;
import ru.trainithard.dunebot.service.telegram.TelegramService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
    private final TelegramService telegramService;
    private final MatchRepository matchRepository;
    private final PlayerMatchRepository playerMatchRepository;
    private final TransactionTemplate transactionTemplate;

    private static final List<String> POLL_OPTIONS = List.of("Да", "Нет", "Результат");

    @Override
    public void requestNewMatch(Player initiator, ModType modType) throws TelegramApiException {
        Match match = new Match();
        match.setModType(modType);
        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setPlayer(initiator);

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
                    playerMatch.setMatch(savedMatch);
                    playerMatchRepository.save(playerMatch);
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
            case UPRISING_4 -> SettingConstants.TOPIC_ID_UPRISING;
            case UPRISING_6 -> SettingConstants.TOPIC_ID_UPRISING;
        };
    }

    @Override
    public void cancelNewMatch(String chatId, int messageId) throws TelegramApiException {

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
