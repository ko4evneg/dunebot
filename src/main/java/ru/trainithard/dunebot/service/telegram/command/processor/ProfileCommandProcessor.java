package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.WrongNamesInputException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.LogId;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.util.ParsedNames;

import java.util.Optional;

/**
 * Register new player, if names provided as command arguments or update existing player names and telegram data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileCommandProcessor extends CommandProcessor {
    private static final String WRONG_INPUT_EXCEPTION_TEXT =
            "Неверный формат ввода имен. Пример верного формата:" + SettingConstants.EXTERNAL_LINE_SEPARATOR +
            "/profile Иван (Лось) Петров";
    private static final String NICKNAME_IS_BUSY_MESSAGE_TEMPLATE = "Пользователь со steam ником '%s' уже существует!";
    private static final String SUCCESSFUL_UPDATE_MESSAGE = "Данные профиля обновлены.";
    private static final String REGISTRATION_MESSAGE_TEMPLATE = "Вы зарегистрированы как '%s'";
    private static final String GUEST_USER_EMPTY_ARGUMENTS_MESSAGE =
            "Вам необходимо подтвердить регистрацию. Используйте команду '/profile Имя (Steam_никнейм) Фамилия'";

    private final PlayerRepository playerRepository;

    @Override
    public void process(CommandMessage commandMessage) {
        log.debug("{}: PROFILE started", logId());

        ParsedNames parsedNames = getValidatedParsedNames(commandMessage);

        long externalUserId = commandMessage.getUserId();
        Optional<Player> playerOptional = playerRepository.findByExternalId(externalUserId);
        log.debug("{}: player found: {}", logId(), playerOptional.isPresent());

        MessageDto messageDto;
        Player player;
        if (playerOptional.isPresent()) {
            player = playerOptional.get();
            validateGuestUser(commandMessage, player);
            updatePlayerExternalData(player, commandMessage);
            if (commandMessage.getArgumentsCount() > 0) {
                updatePlayerNames(player, commandMessage);
                player.setGuest(false);
            }
            messageDto = new MessageDto(commandMessage, new ExternalMessage(SUCCESSFUL_UPDATE_MESSAGE), null);
        } else if (parsedNames != null) {
            validateSteamNameIsFree(parsedNames.getSteamName(), commandMessage);
            player = Player.createRegularPlayer(commandMessage, parsedNames);
            String registrationMessage = String.format(REGISTRATION_MESSAGE_TEMPLATE, player.getFriendlyName());
            messageDto = new MessageDto(commandMessage, new ExternalMessage(registrationMessage), null);
        } else {
            throw new AnswerableDuneBotException(WRONG_INPUT_EXCEPTION_TEXT, commandMessage);
        }

        playerRepository.save(player);
        messagingService.sendMessageAsync(messageDto);

        log.debug("{}: PROFILE started", logId());
    }

    private void validateGuestUser(CommandMessage commandMessage, Player player) {
        if (player.isGuest() && commandMessage.getArgumentsCount() == 0) {
            throw new AnswerableDuneBotException(GUEST_USER_EMPTY_ARGUMENTS_MESSAGE, commandMessage);
        }
    }

    private ParsedNames getValidatedParsedNames(CommandMessage commandMessage) {
        ParsedNames parsedNames = null;
        if (commandMessage.getArgumentsCount() > 0) {
            try {
                String allArguments = commandMessage.getAllArguments();
                parsedNames = new ParsedNames(allArguments);
            } catch (WrongNamesInputException exception) {
                throw new AnswerableDuneBotException(exception.getMessage(), exception, commandMessage);
            }
        }
        return parsedNames;
    }

    private void updatePlayerExternalData(Player player, CommandMessage commandMessage) {
        String firstName = commandMessage.getExternalFirstName();
        String userName = commandMessage.getUserName();
        player.setExternalFirstName(firstName);
        player.setExternalName(userName);
        player.setChatBlocked(false);
        log.debug("{}: player telegram {} names saved (name: {}, username: {})", LogId.get(), player.getId(), firstName, userName);
    }

    private void updatePlayerNames(Player player, CommandMessage commandMessage) {
        try {
            String allArguments = commandMessage.getAllArguments();
            ParsedNames parsedNames = new ParsedNames(allArguments);
            validateSteamNameIsFree(parsedNames.getSteamName(), commandMessage);
            player.setFirstName(parsedNames.getFirstName());
            player.setLastName(parsedNames.getLastName());
            player.setSteamName(parsedNames.getSteamName());
            log.debug("{}: player {} names saved. '{}'", LogId.get(), player.getId(), parsedNames);
        } catch (WrongNamesInputException exception) {
            throw new AnswerableDuneBotException(exception.getMessage(), exception, commandMessage);
        }
    }

    private void validateSteamNameIsFree(String steamName, CommandMessage commandMessage) {
        playerRepository.findBySteamName(steamName)
                .ifPresent(existingPlayer -> {
                    if (commandMessage.getUserId() != existingPlayer.getExternalId()) {
                        String errorMessage = String.format(NICKNAME_IS_BUSY_MESSAGE_TEMPLATE, steamName);
                        throw new AnswerableDuneBotException(errorMessage, commandMessage);
                    }
                });
    }

    @Override
    public Command getCommand() {
        return Command.PROFILE;
    }
}
