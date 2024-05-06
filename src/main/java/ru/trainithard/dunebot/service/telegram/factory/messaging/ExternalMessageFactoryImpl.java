package ru.trainithard.dunebot.service.telegram.factory.messaging;

import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ExternalMessageFactoryImpl implements ExternalMessageFactory {
    @Override
    public ExternalMessage getGuestMessageDto(Player player) {
        return new ExternalMessage("""
                Вас приветствует DuneBot! Вы ответили да в опросе по рейтинговой игре - это значит, что по завершении \
                игры вам придет опрос, где нужно будет указать занятое в игре место (и загрузить скриншот матча в \
                случае победы) - не волнуйтесь, бот подскажет что делать.""").newLine()
                .append("Также вы автоматически зарегистрированы у бота как гость под именем ")
                .append(player.getFirstName()).append(" (").append(player.getSteamName()).append(") ").append(player.getLastName())
                .append(" - это значит, что вы не можете выполнять некоторые команды бота и не будете включены " +
                        "в результаты рейтинга.").newLine()
                .append("Для того, чтобы подтвердить регистрацию, выполните в этом чате команду")
                .appendBold(" '/profile Имя (ник в steam) Фамилия'").append(".").newLine()
                .appendBold("Желательно это  сделать прямо сейчас.").newLine()
                .append("Подробная информация о боте: /help.");
    }

    @Override
    public ExternalMessage getFinishReasonMessage(Match match, boolean isFailedByResubmitsLimit) {
        Long matchId = match.getId();
        ExternalMessage failMessage = new ExternalMessage()
                .startBold().append("Матч ").append(matchId).endBold();
        if (isFailedByResubmitsLimit) {
            return failMessage.append(" завершен без результата, так как превышено максимальное количество попыток регистрации мест " +
                                      "/resubmit. Это может быть вызвано командой или конфликтом мест последнего /resubmit.");
        }
        if (!match.hasAllPlacesSubmitted()) {
            return getFailByMissingSubmitsMessage(match, failMessage);
        }
        if (!match.hasSubmitPhoto()) {
            Player winnerPlayer = match.getMatchPlayers().stream()
                    .filter(matchPlayer -> Objects.requireNonNullElse(matchPlayer.getCandidatePlace(), -1) == 1)
                    .findFirst().orElseThrow().getPlayer();
            return failMessage.append(" завершен без результата, так как игрок ")
                    .appendRaw(MarkdownEscaper.getEscapedMention(winnerPlayer.getMentionTag(), winnerPlayer.getExternalId()))
                    .append(" не загрузил скриншот матча.");
        }
        return failMessage.append(" завершен без результата по неизвестной причине - вероятно это баг.");
    }

    private ExternalMessage getFailByMissingSubmitsMessage(Match match, ExternalMessage failMessage) {
        List<Player> playersWithoutCandidatePlace = match.getMatchPlayers().stream()
                .filter(matchPlayer -> !matchPlayer.hasCandidateVote())
                .map(MatchPlayer::getPlayer)
                .toList();
        List<String> notAnsweredPlayersMentions = new ArrayList<>();
        List<String> chatBlockedPlayersMentions = new ArrayList<>();
        for (Player player : playersWithoutCandidatePlace) {
            String playerMention = MarkdownEscaper.getEscapedMention(player.getMentionTag(), player.getExternalId());
            if (player.isChatBlocked()) {
                chatBlockedPlayersMentions.add(playerMention);
            } else {
                notAnsweredPlayersMentions.add(playerMention);
            }
        }
        failMessage.append(" завершен без результата!");
        if (!notAnsweredPlayersMentions.isEmpty()) {
            failMessage.newLine().append("Игроки ").appendRaw(String.join(", ", notAnsweredPlayersMentions))
                    .append(" не ответили на запрос места.");
        }
        if (!chatBlockedPlayersMentions.isEmpty()) {
            failMessage.newLine().append("Игроки ").appendRaw(String.join(", ", chatBlockedPlayersMentions))
                    .append(" имеют приватный телеграм профиль и не могут получать сообщения без добавления бота в контакты.");
        }

        return failMessage;
    }

    @Override
    public ExternalMessage getStartMessage(Match match, List<String> regularPlayerMentions,
                                           List<String> guestPlayerMentions, List<String> blockedChatGuests) {
        ExternalMessage startMessage = new ExternalMessage()
                .startBold().append("Матч ").append(match.getId()).endBold().append(" собран. Участники:")
                .newLine().appendRaw(String.join(", ", regularPlayerMentions));
        if (!guestPlayerMentions.isEmpty()) {
            startMessage.newLine().newLine().appendBold("Внимание:")
                    .append(" в матче есть незарегистрированные игроки. Они автоматически зарегистрированы " +
                            "под именем Vasya Pupkin и смогут подтвердить результаты матчей для регистрации результатов:")
                    .newLine().appendRaw(String.join(", ", guestPlayerMentions));
        }
        if (!blockedChatGuests.isEmpty()) {
            startMessage.newLine().newLine().appendBold("Особое внимание:")
                    .append(" у этих игроков заблокированы чаты. Без их регистрации и добавлении в контакты бота")
                    .appendBold(" до начала регистрации результатов, завершить данный матч будет невозможно!")
                    .newLine().appendRaw(String.join(", ", blockedChatGuests));
        }
        return startMessage;
    }

}
