package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;
import ru.trainithard.dunebot.service.dto.TelegramUserPollDto;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.PollMessageDto;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchCommandProcessor {
    private final MatchMakingService matchMakingService;
    //    private final MatchSubmitService matchSubmitService;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;

    private static final List<String> POLL_OPTIONS = List.of("Да", "Нет", "Результат");

    public void registerNewMatch(long externalUserId, ModType modType) {
        playerRepository.findByExternalId(externalUserId)
                .ifPresent(player -> messagingService.sendPollAsync(getNewPoll(player, modType))
                        .thenAccept(telegramPollDto -> matchMakingService.registerNewMatch(player, modType, telegramPollDto)));
    }

    private PollMessageDto getNewPoll(Player initiator, ModType modType) {
        return PollMessageDto.builder()
                .text("Игрок " + initiator.getFriendlyName() + " призывает всех на матч в " + modType.getModName())
                .chatId(SettingConstants.CHAT_ID)
                .replyMessageId(getTopicId(modType))
                .options(POLL_OPTIONS)
                .build();
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> SettingConstants.TOPIC_ID_CLASSIC;
            case UPRISING_4, UPRISING_6 -> SettingConstants.TOPIC_ID_UPRISING;
        };
    }

    public void cancelMatch(long externalUserId) {
        playerRepository.findByExternalId(externalUserId).ifPresent(player -> {
            Optional<Match> latestOwnedMatchOptional = matchRepository.findLatestOwnedMatch(player.getId());
            if (latestOwnedMatchOptional.isPresent()) {
                Match latestOwnedMatch = latestOwnedMatchOptional.get();
                if (latestOwnedMatch.isFinished()) {
                    throw new AnswerableDuneBotException("Запрещено отменять завершенные матчи!", player.getExternalChatId());
                }
                messagingService.deletePollAsync(latestOwnedMatch.getExternalPollId());
                matchMakingService.cancelMatch(latestOwnedMatch);
            }
        });
    }

    public void registerMathPlayer(TelegramUserPollDto pollMessage) {
        playerRepository.findByExternalId(pollMessage.telegramUserId()).ifPresent(player ->
                matchRepository.findByExternalPollIdPollId(pollMessage.telegramPollId()).ifPresent(match ->
                        matchMakingService.registerMathPlayer(player, match, pollMessage.positiveAnswersCount())
                )
        );
    }

    public void unregisterMathPlayer(TelegramUserPollDto pollMessage) {
        matchPlayerRepository
                .findByMatchExternalPollIdPollIdAndPlayerExternalId(pollMessage.telegramPollId(), pollMessage.telegramUserId())
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
