package ru.trainithard.dunebot.service.telegram.factory.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import java.util.*;
import java.util.stream.Collectors;

import static ru.trainithard.dunebot.configuration.SettingConstants.EXTERNAL_LINE_SEPARATOR;
import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

@Service
public class ExternalMessageFactoryImpl implements ExternalMessageFactory {
    @Value("${bot.version}")
    private String version;

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

    @Override
    public ExternalMessage getHostMessage(Player hoster, Match match, String server) {
        String mentionsRow = match.getMatchPlayers().stream()
                .map(matchPlayer -> {
                    Player player = matchPlayer.getPlayer();
                    return MarkdownEscaper.getEscapedMention(player.getMentionTag(), player.getExternalId());
                })
                .collect(Collectors.joining(", "));

        return new ExternalMessage()
                .append("Игрок ").append(hoster.getFriendlyName()).append(" предлагает свой сервер для ")
                .startBold().append("матча ").append(match.getId()).endBold().append(".")
                .newLine().append("Сервер: ").appendBold(server).newLine().newLine().appendRaw(mentionsRow);
    }

    @Override
    public ExternalMessage getNonClonflictSubmitMessage(long matchId, int candidatePlace) {
        return candidatePlace == 0
                ? getAcceptedSubmitMessageTemplateForNonParticipant(matchId)
                : getAcceptedSubmitMessageTemplateForParticipant(matchId, candidatePlace);
    }

    private ExternalMessage getAcceptedSubmitMessageTemplateForNonParticipant(long matchId) {
        return new ExternalMessage("В матче ").append(matchId).append(" за вами зафиксирован статус: ")
                .appendBold("не участвует").append(".").newLine()
                .append("При ошибке используйте команду '/resubmit ").append(matchId).append("'.");
    }

    private ExternalMessage getAcceptedSubmitMessageTemplateForParticipant(long matchId, int candidatePlace) {
        return new ExternalMessage("В матче ").append(matchId).append(" за вами зафиксировано ")
                .startBold().append(candidatePlace).append(" место").endBold().append(".").newLine()
                .append("При ошибке используйте команду '/resubmit ").append(matchId).append("'.").newLine()
                .appendBold("Теперь выберите лидера").append(" которым играли.");
    }

    @Override
    public ExternalMessage getConflictSubmitMessage(Collection<MatchPlayer> matchPlayers, MatchPlayer candidate, int candidatePlace) {
        Map<Integer, List<MatchPlayer>> playersByPlace = matchPlayers.stream()
                .filter(matchPlayer -> matchPlayer.getCandidatePlace() != null && !matchPlayer.getId().equals(candidate.getId()))
                .collect(Collectors.groupingBy(MatchPlayer::getCandidatePlace));
        List<MatchPlayer> candidatePlaceMatchPlayers = playersByPlace.computeIfAbsent(candidatePlace, key -> new ArrayList<>());
        candidatePlaceMatchPlayers.add(candidate);
        ExternalMessage conflictMessage = new ExternalMessage("Некоторые игроки не смогли поделить ").startBold();
        for (Map.Entry<Integer, List<MatchPlayer>> entry : playersByPlace.entrySet()) {
            List<MatchPlayer> conflictMatchPlayers = entry.getValue();
            if (conflictMatchPlayers.size() > 1) {
                conflictMessage.append(entry.getKey()).append(" место").endBold().append(":").newLine();
                conflictMatchPlayers.stream()
                        .map(matchPlayer -> matchPlayer.getPlayer().getFriendlyName())
                        .forEach(playerFriendlyName -> conflictMessage.append(playerFriendlyName).newLine());
            }
        }
        conflictMessage.append(EXTERNAL_LINE_SEPARATOR).append("Повторный опрос результата...");

        return conflictMessage;
    }

    @Override
    public ExternalMessage getMatchSuccessfulFinishMessage(Match match) {
        ExternalMessage message = new ExternalMessage();
        message.startBold().append("Матч ").append(match.getId()).endBold().append(" завершился:")
                .append(EXTERNAL_LINE_SEPARATOR).append(EXTERNAL_LINE_SEPARATOR);

        Map<Integer, String> playerNamesByPlace = new LinkedHashMap<>();
        match.getMatchPlayers().stream()
                .filter(matchPlayer -> matchPlayer.getPlace() != null &&
                                       matchPlayer.getPlace() != NOT_PARTICIPATED_MATCH_PLACE)
                .sorted(Comparator.comparing(MatchPlayer::getPlace))
                .forEach(matchPlayer -> playerNamesByPlace.put(matchPlayer.getPlace(), matchPlayer.getPlayer().getFriendlyName()));
        playerNamesByPlace.forEach((place, name) -> {
            message.append(getPlaceEmoji(place)).append(" ");
            if (place.equals(1)) {
                message.append("🥳🍾🎉 ").append(name).append(" 🎉🍾🥳");
            } else {
                message.append(name);
            }
            message.append(EXTERNAL_LINE_SEPARATOR);
        });
        message.trimTrailingNewLine();
        return message;
    }

    private String getPlaceEmoji(Integer place) {
        return switch (place) {
            case 1 -> "1️⃣";
            case 2 -> "2️⃣";
            case 3 -> "3️⃣";
            case 4 -> "4️⃣";
            case 5 -> "5️⃣";
            case 6 -> "6️⃣";
            default -> throw new IllegalArgumentException("Can't determine place number emoji");
        };
    }

    @Override
    public ExternalMessage getAcceptSubmitRejectedDueToMatchFinishMessage(long matchId) {
        return new ExternalMessage().startBold().append("Матч ").append(matchId).endBold()
                .append(" уже завершен. Регистрация вашего голоса невозможна.");
    }

    @Override
    public ExternalMessage getPreSubmitTimeoutNotificationMessage(Match match) {
        if (MatchState.getEndedMatchStates().contains(match.getState())) {
            return null;
        }
        String playerTags = match.getMatchPlayers().stream()
                .filter(matchPlayer -> matchPlayer.getCandidatePlace() == null)
                .map(matchPlayer -> {
                    Player player = matchPlayer.getPlayer();
                    return MarkdownEscaper.getEscapedMention(player.getMentionTag(), player.getExternalId());
                })
                .collect(Collectors.joining(", "));
        return new ExternalMessage().appendBold("⚠️ Внимание: ").append("осталось 10 минут на публикацию результатов ")
                .startBold().append("матча ").append(match.getId()).endBold().append("!").newLine()
                .append("Результат еще не зарегистрировали игроки:").newLine()
                .appendRaw(playerTags);
    }

    @Override
    public ExternalMessage getPlayersSubmitMessage(long matchId) {
        return new ExternalMessage("Регистрация результатов для ")
                .startBold().append("матча ").append(matchId).endBold()
                .append(". Нажмите по очереди кнопки с именами участвовавших игроков, " +
                        "начиная от победителя и заканчивая последним местом.");
    }

    @Override
    public ExternalMessage getLeadersSubmitMessage(long matchId) {
        return new ExternalMessage("Теперь выберите лидеров для ")
                .startBold().append("матча ").append(matchId).endBold()
                .append(". Нажмите по очереди кнопки с именами лидеров, " +
                        "начиная от лидера победителя и заканчивая лидером, занявшим последнее место.");
    }

    @Override
    public ExternalMessage getFinishedPlayersSubmitMessage(Collection<MatchPlayer> matchPlayers) {
        Long matchId = matchPlayers.stream().findFirst().orElseThrow().getMatch().getId();
        String orderedParticipants = matchPlayers.stream()
                .sorted(Comparator.comparing(matchPlayer -> Objects.requireNonNull(matchPlayer.getPlace())))
                .map(matchPlayer -> matchPlayer.getPlace() + ": " + matchPlayer.getPlayer().getFriendlyName())
                .collect(Collectors.joining(EXTERNAL_LINE_SEPARATOR));
        return new ExternalMessage("Следующие результаты зарегистрированы для ")
                .startBold().append("матча ").append(matchId).endBold()
                .append(":").newLine().append(orderedParticipants);
    }

    @Override
    public ExternalMessage getHelpMessage() {
        return new ExternalMessage()
                .startBold().append("Dunebot v").append(version).endBold().newLine().newLine()
                .append("================================").newLine()
                .appendBoldLink("Подробная инструкция к боту", "https://github.com/ko4evneg/dunebot/blob/master/help.md").newLine()
                .append("================================").newLine().newLine()
                .appendBold("Краткая инструкция").newLine().newLine()
                .appendBold("‼️Все команды пишем напрямую в чат бота ").appendBold("@tabledune_bot").newLine().newLine()
                .appendBold("1️⃣  Регистрация").newLine()
                .appendInline("/profile Имя (ник_steam) Фамилия").newLine()
                .append("'Имя' и 'Фамилия' - это ваши данные для рейтинга, 'ник_steam' - ваш ник в Steam. ")
                .append("Писать в таком же формате как и указано - имя, ник стима в скобочках, фамилия.").newLine()
                .append("🪧  Для смены данных выполняется та же команда, что и выше.").newLine()
                .append("📌  ").appendInline("/profile")
                .append(" - обновляет имена из Telegram профиля (доступна только после регистрации).")
                .newLine().newLine()
                .appendBold("2️⃣  Создание матча").newLine()
                .appendInline("/new_dune").append(" - для классики").newLine()
                .appendInline("/new_up4").append(" - для обычного Uprising").newLine()
                .appendInline("/new_up6").append(" - для Uprising 3х3 (для этого режима бот только создает опросы)")
                .newLine().newLine()
                .appendBold("3️⃣  Начало матча").newLine()
                .append("Ждем, пока найдутся все игроки - бот пришлет уведомление в канал и тегнет вас. ")
                .append("В уведомлении вы найдете ").appendBold("ID матча").append(" - он понадобится для публикации результатов.")
                .newLine().newLine()
                .appendBold("4️⃣  Завершение матча").newLine()
                .append("Любой игрок выполняет команду ").appendInline("/submit X")
                .append(", где X - ID матча из пункта 3. ")
                .append("Каждому игроку придет сообщение с кнопками для выбора занятого места и лидера. ")
                .append("Победителю также придет запрос на загрузку скриншота. Скриншот можно просто перетащить в чат.")
                .newLine().newLine()
                .appendBold("5️⃣  Результаты").newLine()
                .append("В канал матчей бота придет результат матча с занятыми местами ")
                .append("- это значит, что все хорошо и матч зачтен в рейтинг. ")
                .append("Иначе придет уведомление, что матч завершен без результата, а также причина ошибки.")
                .newLine().newLine()
                .append("❗  На этапе пилота важно отслеживать все ошибки. " +
                        "Если видите, что бот работает как-то не так, пишите в канал фидбека бота.");
    }
}
