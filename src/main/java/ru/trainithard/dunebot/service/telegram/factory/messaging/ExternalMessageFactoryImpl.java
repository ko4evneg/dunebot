package ru.trainithard.dunebot.service.telegram.factory.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.PlayerRating;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.util.EmojiRandomizer;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.trainithard.dunebot.configuration.SettingConstants.EXTERNAL_LINE_SEPARATOR;

@Service
public class ExternalMessageFactoryImpl implements ExternalMessageFactory {
    private static final String LEADER_EMOJI = "⭐️";
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
    public ExternalMessage getPartialSubmittedMatchFinishMessage(Match match) {
        Player submitter = match.getSubmitter();
        String submitterMention = MarkdownEscaper.getEscapedMention(submitter.getMentionTag(), submitter.getExternalId());
        return new ExternalMessage().startBold().append("Матч ").append(match.getId()).endBold()
                .append(" завершен без результата, так как игрок ").appendRaw(submitterMention)
                .append(" не закончил регистрацию результата.");
    }

    @Override
    public ExternalMessage getFailByResubmitLimitExceededMessage(long matchId) {
        return new ExternalMessage()
                .startBold().append("Матч ").append(matchId).endBold()
                .append(" завершен без результата, так как превышено максимальное количество попыток регистрации результатов.");
    }

    @Override
    public ExternalMessage getResubmitMessage() {
        return new ExternalMessage("Если вы знаете все места и лидеров, вы можете выполнить перерегистрацию результата самостоятельно, " +
                                   "иначе - запрос будет отправлен игроку, выполнившему предыдущую регистрацию результатов.");
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
    public ExternalMessage getMatchSuccessfulFinishMessage(Match match) {
        ExternalMessage message = new ExternalMessage();
        message.startBold().append("Матч ").append(match.getId()).endBold().append(" завершился:")
                .append(EXTERNAL_LINE_SEPARATOR).append(EXTERNAL_LINE_SEPARATOR);

        match.getMatchPlayers().stream()
                .filter(MatchPlayer::hasRateablePlace)
                .sorted(Comparator.comparing(MatchPlayer::getPlace))
                .forEach(matchPlayer -> {
                    String friendlyName = matchPlayer.getPlayer().getFriendlyName();
                    String leaderName = matchPlayer.getLeader().getName();
                    Integer place = matchPlayer.getPlace();
                    String playerNameRaw = place == 1
                            ? EmojiRandomizer.getWinnerEmoji() + " " + friendlyName + " " + EmojiRandomizer.getWinnerEmoji()
                            : friendlyName;
                    String placeEmoji = getPlaceEmoji(place);
                    message.append(placeEmoji).space().append(playerNameRaw).newLine()
                            .append(LEADER_EMOJI).space().append(leaderName).newLine().newLine();
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
    public ExternalMessage getPreSubmitTimeoutNotificationMessage(Match match, int timeout) {
        ExternalMessage message = new ExternalMessage()
                .appendBold("⚠️ Внимание: ").append("осталось ").append(timeout).append(" минут на публикацию результатов ")
                .startBold().append("матча ").append(match.getId()).endBold().append("!").newLine();
        Player submitter = match.getSubmitter();
        String submitterMention = MarkdownEscaper.getEscapedMention(submitter.getMentionTag(), submitter.getExternalId());
        message.append("Игрок ").appendRaw(submitterMention).append(" начал процесс регистрации, но еще не закончил его.");
        return message;
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
                .filter(MatchPlayer::hasRateablePlace)
                .sorted(Comparator.comparing(matchPlayer -> Objects.requireNonNull(matchPlayer.getPlace())))
                .map(matchPlayer -> matchPlayer.getPlace() + ": " + matchPlayer.getPlayer().getFriendlyName())
                .collect(Collectors.joining(EXTERNAL_LINE_SEPARATOR));
        return new ExternalMessage("Следующие результаты зарегистрированы для ")
                .startBold().append("матча ").append(matchId).endBold()
                .append(":").newLine().append(orderedParticipants);
    }

    @Override
    public ExternalMessage getFinishedLeadersSubmitMessage(Collection<MatchPlayer> matchPlayers) {
        Long matchId = matchPlayers.stream().findFirst().orElseThrow().getMatch().getId();
        String orderedParticipants = matchPlayers.stream()
                .filter(MatchPlayer::hasRateablePlace)
                .sorted(Comparator.comparing(matchPlayer -> Objects.requireNonNull(matchPlayer.getPlace())))
                .map(matchPlayer -> {
                    int place = matchPlayer.getPlace();
                    String playerName = matchPlayer.getPlayer().getFriendlyName();
                    String leaderName = matchPlayer.getLeader().getName();
                    return String.format("%d: %s - %s", place, playerName, leaderName);
                })
                .collect(Collectors.joining(EXTERNAL_LINE_SEPARATOR));
        return new ExternalMessage("Следующие результаты зарегистрированы для ")
                .startBold().append("матча ").append(matchId).endBold()
                .append(":").newLine().append(orderedParticipants).newLine().newLine()
                .append("В случае ошибки используйте команду '/resubmit 15000'");
    }

    @Override
    public ExternalMessage getFinishedSubmitParticipantMessage(MatchPlayer matchPlayer, String submitter, int acceptSubmitTimeout) {
        Long matchId = matchPlayer.getMatch().getId();
        ExternalMessage message = new ExternalMessage()
                .append("Игрок ").appendBold(submitter).append(" завершил регистрацию результатов ").startBold()
                .append("матча ").append(matchId).endBold().newLine()
                .append("Ознакомьтесь с результатами - у вас есть ").append(acceptSubmitTimeout)
                .append(" минута чтобы проверить их. В случае ошибки, используйте команду '/resubmit ")
                .append(matchId).append("'.").newLine().newLine();
        if (matchPlayer.hasRateablePlace()) {
            Integer place = matchPlayer.getPlace();
            String leader = matchPlayer.getLeader().getName();
            message.append("За вами зарегистрированы ").startBold().append(place).append(" место").endBold()
                    .append(" и лидер ").appendBold(leader).append(".");
        } else {
            message.append("За вами зарегистрировано ").appendBold("неучастие").append(" в матче.");
        }
        return message;
    }

    @Override
    public ExternalMessage getNoRatingsMessage() {
        return new ExternalMessage("Рейтинг за текущий месяц еще не рассчитан, повторите запрос завтра.");
    }

    @Override
    public ExternalMessage getNoOwnedRatingsMessage() {
        return new ExternalMessage("В рейтинге за текущий месяц нет матчей с вашим участием. Перерасчет рейтинга будет выполнен ночью.");
    }

    @Override
    public ExternalMessage getRatingStatsMessage(int startingPlace, List<PlayerRating> sortedRatings, Player requestingPlayer) {
        ExternalMessage message = new ExternalMessage("📋 Статистика текущего месяца").newLine().newLine();
        ExternalMessage playersMessage = new ExternalMessage();
        int currentPlace = startingPlace;
        for (int i = 0; i < sortedRatings.size(); i++) {
            PlayerRating currentRating = sortedRatings.get(i);
            String name = currentRating.getPlayer().getFriendlyName();
            String efficiency = String.format("%.2f", currentRating.getEfficiency());
            if (requestingPlayer.equals(currentRating.getPlayer())) {
                message.append("Сыграно матчей: ").append(currentRating.getMatchesCount()).newLine()
                        .append("Текущий страйк: ").append(currentRating.getCurrentStrikeLength()).newLine()
                        .append("Максимальный страйк: ").append(currentRating.getMaxStrikeLength()).newLine().newLine();
                playersMessage.startBold().append(currentPlace).append(". ").append(name).append("  |  ")
                        .append(efficiency).endBold().newLine();
            } else {
                playersMessage.append(currentPlace).append(". ").append(name).append("  |  ").append(efficiency).newLine();
            }
            currentPlace++;
        }
        return message.concat(playersMessage).trimTrailingNewLine();
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
                .append("Этому игроку придут запросы со списком участников игры и лидеров матча. ")
                .append("В каждом запросе необходимо будет нажать кнопки игроков или лидеров в порядке очередности их мест, ")
                .append("начиная с первого места, и заканчивая последним.")
                .newLine().newLine()
                .appendBold("5️⃣  Подтверждение результатов").newLine()
                .append("Каждый участник получит сообщения с назначенным ему местом и лидером. ")
                .append("В течение небольшого времени, после завершения матча, у всех игроков есть возможность исправить " +
                        "неверные результаты, перезапустив их регистрацию командой ").appendInline("/resubmit X")
                .newLine().newLine()
                .appendBold("6️⃣  Результаты").newLine()
                .append("В канал матчей бота придет результат матча с занятыми местами ")
                .append("- это значит, что все хорошо и матч зачтен в рейтинг. ")
                .append("Иначе придет уведомление, что матч завершен без результата, а также причина ошибки.");
    }
}
