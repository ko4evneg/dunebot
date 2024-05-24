package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.*;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.AppSettingsService;
import ru.trainithard.dunebot.service.UserSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostCommandProcessor extends CommandProcessor {
    private static final String NO_SETTING_FOUND_MESSAGE =
            "Для использования команды необходимо сохранить ваши данные. Ознакомьтесь с разделом 'Хосты' в полной справке";
    private final MatchRepository matchRepository;
    private final UserSettingsService userSettingsService;
    private final PlayerRepository playerRepository;
    private final AppSettingsService appSettingsService;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: HOST started", logId());
        playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player ->
                matchRepository.findLatestPlayerMatch(player.getId(), List.of(MatchState.NEW)).ifPresent(match ->
                        userSettingsService.getSetting(player.getId(), UserSettingKey.HOST)
                                .ifPresentOrElse(setting -> {
                                    log.debug("{}: found setting {} for player {} match {}",
                                            logId(), setting.getId(), player.getId(), match.getId());
                                    AppSettingKey appSettingKey = match.getModType() == ModType.CLASSIC ?
                                            AppSettingKey.TOPIC_ID_CLASSIC : AppSettingKey.TOPIC_ID_UPRISING;
                                    Integer matchTopic = appSettingsService.getIntSetting(appSettingKey);
                                    String chatId = appSettingsService.getStringSetting(AppSettingKey.CHAT_ID);
                                    String server = setting.getValue();
                                    ExternalMessage externalMessage = getServerMessage(player, match, server);
                                    Integer pollId = match.getExternalPollId().getMessageId();
                                    MessageDto messageDto = new MessageDto(chatId, externalMessage, matchTopic, pollId, null);
                                    messagingService.sendMessageAsync(messageDto);
                                }, () -> {
                                    throw new AnswerableDuneBotException(NO_SETTING_FOUND_MESSAGE, commandMessage);
                                })
                )
        );
        log.debug("{}: HOST ended", logId());
    }

    private ExternalMessage getServerMessage(Player player, Match match, String server) {
        return new ExternalMessage()
                .append("Игрок ").append(player.getFriendlyName()).append(" предлагает свой сервер для ")
                .startBold().append("матча ").append(match.getId()).endBold().append(".")
                .newLine().append("Сервер: ").appendBold(server);
    }

    @Override
    public Command getCommand() {
        return Command.HOST;
    }
}
