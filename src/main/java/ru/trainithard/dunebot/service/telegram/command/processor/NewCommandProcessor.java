package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

/**
 * Creates new poll in external messaging system for new match gathering.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewCommandProcessor extends CommandProcessor {
    private static final String POSITIVE_ANSWER = "Да";
    private static final List<String> POLL_OPTIONS = List.of(POSITIVE_ANSWER, "Нет", "Результат");
    private static final String NEW_POLL_MESSAGE_TEMPLATE = "Игрок %s призывает всех на матч в %s";
    private static final String UNSUPPORTED_MATCH_TYPE_MESSAGE_TEMPLATE = "Неподдерживаемый тип матча: %s";

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final MessagingService messagingService;
    private final SettingsService settingsService;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        log.debug("{}: new started", loggingId);

        String modTypeString = commandMessage.getArgument(1);
        ModType modType = ModType.getByAlias(modTypeString);
        if (modType == null) {
            throw new AnswerableDuneBotException(String.format(UNSUPPORTED_MATCH_TYPE_MESSAGE_TEMPLATE, modTypeString), commandMessage);
        }
        playerRepository.findByExternalId(commandMessage.getUserId())
                .ifPresent(player -> messagingService.sendPollAsync(getNewPollMessage(player, modType))
                        .thenAccept(telegramPollDto -> {
                            Match match = new Match(modType);
                            match.setExternalPollId(telegramPollDto.toExternalPollId());
                            match.setOwner(player);
                            matchRepository.save(match);
                        }));

        log.debug("{}: new ended", loggingId);
    }

    private PollMessageDto getNewPollMessage(Player initiator, ModType modType) {
        String text = String.format(NEW_POLL_MESSAGE_TEMPLATE, initiator.getFriendlyName(), modType.getModName());
        String chatId = settingsService.getStringSetting(SettingKey.CHAT_ID);
        return new PollMessageDto(chatId, text, getTopicId(modType), POLL_OPTIONS);
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> settingsService.getIntSetting(SettingKey.TOPIC_ID_CLASSIC);
            case UPRISING_4, UPRISING_6 -> settingsService.getIntSetting(SettingKey.TOPIC_ID_UPRISING);
        };
    }

    @Override
    public Command getCommand() {
        return Command.NEW;
    }
}
