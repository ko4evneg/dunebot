package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

@Service
@RequiredArgsConstructor
public class HostCommandProcessor extends CommandProcessor {
    private final MatchRepository matchRepository;
    private final UserSettingsService userSettingsService;
    private final PlayerRepository playerRepository;
    private final AppSettingsService appSettingsService;

    @Override
    public void process(CommandMessage commandMessage) {
        playerRepository.findByExternalId(commandMessage.getUserId()).ifPresent(player ->
                matchRepository.findLatestPlayerMatch(player.getId(), List.of(MatchState.NEW)).ifPresent(match ->
                        userSettingsService.getSetting(player.getId(), UserSettingKey.HOST).ifPresent(setting -> {
                            AppSettingKey appSettingKey = match.getModType() == ModType.CLASSIC ?
                                    AppSettingKey.TOPIC_ID_CLASSIC : AppSettingKey.TOPIC_ID_UPRISING;
                            Integer matchTopic = appSettingsService.getIntSetting(appSettingKey);
                            String chatId = appSettingsService.getStringSetting(AppSettingKey.CHAT_ID);
                            String server = setting.getValue();
                            ExternalMessage externalMessage = getServerMessage(player, match, server);
                            MessageDto messageDto = new MessageDto(chatId, externalMessage, matchTopic, null);
                            messagingService.sendMessageAsync(messageDto);
                        })
                )
        );
    }

    private ExternalMessage getServerMessage(Player player, Match match, String server) {
        return new ExternalMessage()
                .append("Игрок ").append(player.getFriendlyName()).append(" предлагает свой сервер для ")
                .startBold().append("матча ").append(match.getId()).endBold().append(".")
                .newLine().append("Сервер: ").append(server);
    }

    @Override
    public Command getCommand() {
        return Command.HOST;
    }
}
