package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.MatchMakingService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewCommandProcessor implements CommandProcessor {
    private final PlayerRepository playerRepository;
    private final MatchMakingService matchMakingService;
    private final MessagingService messagingService;

    public static final String POSITIVE_ANSWER = "Да";
    private static final List<String> POLL_OPTIONS = List.of(POSITIVE_ANSWER, "Нет", "Результат");

    @Override
    public void process(CommandMessage commandMessage) {
        String modTypeString = commandMessage.getArgument(1);
        ModType modType = ModType.getByAlias(modTypeString);
        if (modType == null) {
            throw new AnswerableDuneBotException("Неподдерживаемый тип матча: " + modTypeString, commandMessage.getChatId(), commandMessage.getReplyMessageId());
        }
        playerRepository.findByExternalId(commandMessage.getUserId())
                .ifPresent(player -> messagingService.sendPollAsync(getNewPollMessage(player, modType))
                        .thenAccept(telegramPollDto -> matchMakingService.registerNewMatch(player, modType, telegramPollDto)));
    }

    private PollMessageDto getNewPollMessage(Player initiator, ModType modType) {
        String text = String.format("Игрок %s призывает всех на матч в %s", initiator.getFriendlyName(), modType.getModName());
        return new PollMessageDto(SettingConstants.CHAT_ID, text, getTopicId(modType), POLL_OPTIONS);
    }

    private int getTopicId(ModType modType) {
        return switch (modType) {
            case CLASSIC -> SettingConstants.TOPIC_ID_CLASSIC;
            case UPRISING_4, UPRISING_6 -> SettingConstants.TOPIC_ID_UPRISING;
        };
    }

    @Override
    public Command getCommand() {
        return Command.NEW;
    }
}
