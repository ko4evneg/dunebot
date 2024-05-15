package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.UserSetting;
import ru.trainithard.dunebot.model.UserSettingKey;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.UserSettingsService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigCommandProcessor extends CommandProcessor {
    private static final String SHOW_SUBCOMMAND = "SHOW";
    private final UserSettingsService userSettingsService;
    private final PlayerRepository playerRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        String[] args = commandMessage.getAllArguments().split("\\s");
        String subCommand = args[0];
        Optional<Player> playerOptional = playerRepository.findByExternalId(commandMessage.getUserId());
        if (playerOptional.isEmpty()) {
            return;
        }

        Player player = playerOptional.get();
        if (SHOW_SUBCOMMAND.equalsIgnoreCase(subCommand)) {
            Map<UserSettingKey, String> playerSettingsByKey = userSettingsService.getAllSettings(player.getId()).stream()
                    .collect(Collectors.toMap(UserSetting::getKey, UserSetting::getValue));
            ExternalMessage externalMessage = new ExternalMessage("Ваши настройки:").newLine();
            if (playerSettingsByKey.isEmpty()) {
                externalMessage.append("настроек нет");
            } else {
                playerSettingsByKey.forEach((key, value) -> externalMessage.appendBold(key).append(": ").append(value));
            }
            messagingService.sendMessageAsync(new MessageDto(commandMessage, externalMessage, null));
        } else {
            UserSettingKey userSettingKey = UserSettingKey.getByName(subCommand)
                    .orElseThrow(() -> new AnswerableDuneBotException("Неверный аргумент!", commandMessage));
            String commandArgument = commandMessage.getAllArguments().substring(subCommand.length()).trim();
            userSettingsService.saveSetting(player.getId(), userSettingKey, commandArgument);
            messagingService.sendMessageAsync(new MessageDto(commandMessage, new ExternalMessage("Настройка сохранена"), null));
        }
    }

    @Override
    public Command getCommand() {
        return Command.CONFIG;
    }
}
