package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.DubeBotExsception;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.TelegramUserMessageDto;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static ru.trainithard.dunebot.configuration.SettingConstants.CHAT_ID;

@Service
@RequiredArgsConstructor
public class TextCommandProcessor {
    private final MatchMakingService matchMakingService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final TelegramBot telegramBot;

    private static final List<String> POLL_OPTIONS = List.of("Да", "Нет", "Результат");

    public void registerNewMatch(long telegramUserId, ModType modType) {
        playerRepository.findByTelegramId(telegramUserId).ifPresent(player -> {
            try {
                CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(getNewPoll(player, modType));
                TelegramUserMessageDto telegramUserMessage = new TelegramUserMessageDto();
                sendMessageCompletableFuture.whenComplete((message, throwable) -> {
                    if (throwable == null) {
                        telegramUserMessage.setTelegramPollId(message.getPoll().getId());
                        telegramUserMessage.setTelegramMessageId(message.getMessageId());
                        telegramUserMessage.setTelegramChatId(message.getChatId());
                        matchMakingService.registerNewMatch(player, modType, telegramUserMessage);
                    } else {
                        // TODO: handle?
                        throw new TelegramApiCallException("sendPoll() call encounters API exception", throwable);
                    }
                });
            } catch (Exception exception) {
                throw new DubeBotExsception(exception);
            }
        });
    }

    private SendPoll getNewPoll(Player initiator, ModType modType) {
        SendPoll sendPoll = new SendPoll();
        sendPoll.setQuestion("Игрок " + initiator.getFriendlyName() + " призывает всех на матч в " + modType.getModName());
        sendPoll.setAllowMultipleAnswers(false);
        sendPoll.setIsAnonymous(false);
        sendPoll.setChatId(CHAT_ID);
        sendPoll.setReplyToMessageId(getTopicId(modType));
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

    public void cancelMatch(long telegramUserId) {
        playerRepository.findByTelegramId(telegramUserId).ifPresent(player -> {
            Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(player.getId());
            Match latestOwnedMatch = latestOwnedMatchOptional.get();
            if (latestOwnedMatch.isFinished()) {
                // TODO:  notify
                return;
            }
            if (latestOwnedMatchOptional.isPresent()) {
                try {
                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setMessageId(latestOwnedMatch.getTelegramMessageId());
                    deleteMessage.setChatId(latestOwnedMatch.getTelegramChatId());
                    telegramBot.executeAsync(deleteMessage);
                    matchMakingService.cancelMatch(latestOwnedMatch);
                } catch (TelegramApiException exception) {
                    throw new TelegramApiCallException("deleteMessage() encounters API exception", exception);
                }
            }
        });
    }

    public void registerMathPlayer(TelegramUserPollDto pollMessage) {
        playerRepository.findByTelegramId(pollMessage.telegramUserId()).ifPresent(player ->
                matchRepository.findByTelegramPollId(pollMessage.telegramPollId()).ifPresent(match ->
                        matchMakingService.registerMathPlayer(player, match)
                )
        );
    }

    public void unregisterMathPlayer(TelegramUserPollDto pollMessage) {
        matchPlayerRepository
                .findByMatchTelegramPollIdAndPlayerTelegramId(pollMessage.telegramPollId(), pollMessage.telegramUserId())
                .ifPresent(matchMakingService::unregisterMathPlayer);
    }
}