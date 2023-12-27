package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.DubeBotException;
import ru.trainithard.dunebot.exception.TelegramApiCallException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;
import ru.trainithard.dunebot.service.dto.TelegramUserMessageDto;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static ru.trainithard.dunebot.configuration.SettingConstants.CHAT_ID;

@Service
@RequiredArgsConstructor
public class MatchCommandProcessor {
    private final MatchMakingService matchMakingService;
    //    private final MatchSubmitService matchSubmitService;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final TelegramBot telegramBot;

    private static final List<String> POLL_OPTIONS = List.of("Да", "Нет", "Результат");

    public void registerNewMatch(long telegramUserId, ModType modType) {
        playerRepository.findByTelegramId(telegramUserId).ifPresent(player -> {
            try {
                CompletableFuture<Message> sendMessageCompletableFuture = telegramBot.executeAsync(getNewPoll(player, modType));
                sendMessageCompletableFuture.whenComplete((message, throwable) -> {
                    if (throwable != null) {
                        // TODO: handle?
                        throw new TelegramApiCallException("sendPoll() call encounters API exception", throwable);
                    }
                    matchMakingService.registerNewMatch(player, modType, new TelegramUserMessageDto(message));
                });
            } catch (Exception exception) {
                throw new DubeBotException(exception);
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
            if (latestOwnedMatchOptional.isPresent()) {
                Match latestOwnedMatch = latestOwnedMatchOptional.get();
                if (latestOwnedMatch.isFinished()) {
                    throw new AnswerableDuneBotException("Запрещено отменять завершенные матчи!", player.getTelegramChatId());
                }
                try {
                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setMessageId(latestOwnedMatch.getTelegramMessageId().getMessageId());
                    deleteMessage.setChatId(latestOwnedMatch.getTelegramMessageId().getChatId());
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
                        matchMakingService.registerMathPlayer(player, match, pollMessage.positiveAnswersCount())
                )
        );
    }

    public void unregisterMathPlayer(TelegramUserPollDto pollMessage) {
        matchPlayerRepository
                .findByMatchTelegramPollIdAndPlayerTelegramId(pollMessage.telegramPollId(), pollMessage.telegramUserId())
                .ifPresent(matchPlayer -> matchMakingService.unregisterMathPlayer(matchPlayer, pollMessage.positiveAnswersCount()));
    }

    public void registerNewPlayer(PlayerRegistrationDto playerRegistration) {
        playerService.registerNewPlayer(playerRegistration);
    }
// TODO:
//    public void getSubmitMessage(long telegramUserId, long telegramChatId, String matchIdString) throws TelegramApiException {
//        MatchSubmitDto matchSubmit = matchSubmitService.getMatchSubmit(telegramUserId, telegramChatId, matchIdString);
//        List<InlineKeyboardButton> buttons = new ArrayList<>();
//        List<Long> matchPlayerTelegramIds = matchSubmit.telegramPlayerIds();
//        for (int i = 0; i < matchPlayerTelegramIds.size() ; i++) {
//            buttons.add(InlineKeyboardButton.builder().text(Integer.toString(i + 1)).callbackData(matchIdString + "__" + (i + 1)).build());
//        }
//        buttons.add(InlineKeyboardButton.builder().text("не участвовал(а)").callbackData(matchIdString + "__-1").build());
//        List<List<InlineKeyboardButton>> linedButtons = Lists.partition(buttons, 2);
//
//        String telegramChatIdString = Long.toString(telegramChatId);
//        SendMessage sendMessage = new SendMessage(telegramChatIdString, "Выберите место, которое вы заняли в матче " + matchSubmit.modType() + ":");
//        sendMessage.setReplyMarkup(new InlineKeyboardMarkup(linedButtons));
//
//        CompletableFuture<Message> messageCompletableFuture = telegramBot.executeAsync(sendMessage);
//        messageCompletableFuture.whenComplete((message, throwable) -> {
//            if (throwable != null) {
//                throw new TelegramApiCallException("getSubmitMessage() call encounters API exception", throwable);
//            }
//        });
//    }
}
