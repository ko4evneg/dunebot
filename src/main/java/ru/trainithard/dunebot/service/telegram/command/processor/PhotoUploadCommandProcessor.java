package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.exception.ScreenshotFileIOException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchPlayer;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.ScreenshotService;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.TelegramFileDetailsDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.KeyboardsFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ru.trainithard.dunebot.configuration.SettingConstants.MAX_SCREENSHOT_SIZE;

/**
 * Accepts external messaging system photo upload of match results screenshot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoUploadCommandProcessor extends CommandProcessor {
    private static final String FILE_DOWNLOAD_URI_PREFIX = "https://api.telegram.org/file/bot";
    private static final String PATH_SEPARATOR = "/";
    private static final String UNSUPPORTED_UPDATE_TYPE = "Unsupported photo/document update type";
    private static final String SUCCESSFUL_UPLOAD_TEXT = "Скриншот успешно загружен.";

    private final RestTemplate restTemplate;
    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final ScreenshotService screenshotService;
    private final MatchFinishingService matchFinishingService;
    private final KeyboardsFactory keyboardsFactory;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public void process(CommandMessage commandMessage) {
        int logId = logId();
        log.debug("{}: PHOTO started", logId);

        List<MatchPlayer> matchPlayersWithOnSubmitMatches =
                matchPlayerRepository.findByPlayerExternalIdAndMatchState(commandMessage.getUserId(), MatchState.ON_SUBMIT);
        if (matchPlayersWithOnSubmitMatches.size() > 1) {
            throw new AnswerableDuneBotException("У вас несколько матчей в состоянии /submit. Вероятно это баг.", commandMessage);
        }
        MatchPlayer matchPlayer = matchPlayersWithOnSubmitMatches.get(0);
        log.debug("{}: matchPlayer {}, match {} found", logId, matchPlayer.getId(), matchPlayer.getMatch().getId());

        String fileId = getFileId(commandMessage);
        log.debug("{}: file telegram id: {}", logId, fileId);
        CompletableFuture<TelegramFileDetailsDto> file = messagingService.getFileDetails(fileId);
        file.whenComplete((telegramFileDetailsDto, throwable) ->
                processTelegramFile(commandMessage, telegramFileDetailsDto, logId, matchPlayer));

        log.debug("{}: PHOTO ended", logId);
    }

    private void processTelegramFile(CommandMessage commandMessage, TelegramFileDetailsDto telegramFileDetailsDto,
                                     int logId, MatchPlayer matchPlayer) {
        log.debug("{}: file detail callback received from telegram", logId);

        String filePath = telegramFileDetailsDto.path();
        byte[] photoBytes = restTemplate.getForObject(getFileUri(filePath), byte[].class);
        log.debug("{}: file bytes[] received from telegram", logId);

        try {
            if (photoBytes != null) {
                Match match = matchPlayer.getMatch();
                String dottedFileExtension = getFileExtension(filePath);
                String savePath = screenshotService.save(match.getId(), dottedFileExtension, photoBytes);
                log.debug("{}: save path: {}", logId, savePath);
                match.setScreenshotPath(savePath);
                match.setState(MatchState.ON_SUBMIT_SCREENSHOTTED);
                matchRepository.save(match);
                log.debug("{}: match photo flag saved", logId);
                if (match.canBePreliminaryFinished()) {
                    log.debug("{}: match {} finishing ({})", logId, match.getId(), getMatchLogInfo(match));
                    matchFinishingService.finishSubmittedMatch(match.getId());
                }

                List<List<ButtonDto>> leadersKeyboard = keyboardsFactory.getLeadersKeyboard(matchPlayer);
                MessageDto message = new MessageDto(commandMessage, new ExternalMessage(SUCCESSFUL_UPLOAD_TEXT), leadersKeyboard);
                messagingService.sendMessageAsync(message);
            }
        } catch (ScreenshotFileIOException exception) {
            log.error(logId + ": file save failed due to an exception", exception);
            messagingService.sendMessageAsync(new MessageDto(commandMessage, new ExternalMessage(exception.getMessage()), null));
        } catch (IOException exception) {
            log.error(logId + ": encountered an exception", exception);
        }
    }

    private String getFileId(CommandMessage commandMessage) {
        if (commandMessage.getPhoto() != null) {
            CommandMessage.Photo largestPhoto = commandMessage.getPhoto().stream()
                    .filter(photo -> photo.size() <= MAX_SCREENSHOT_SIZE)
                    .max(Comparator.comparing(CommandMessage.Photo::size))
                    .orElseThrow();
            return largestPhoto.id();
        } else if (commandMessage.getFile() != null) {
            return commandMessage.getFile().id();
        }
        throw new DuneBotException(UNSUPPORTED_UPDATE_TYPE);
    }

    private String getFileUri(String filePath) {
        String effectiveFilePath = filePath.startsWith(PATH_SEPARATOR) ? filePath : PATH_SEPARATOR + filePath;
        String fileUri = FILE_DOWNLOAD_URI_PREFIX + botToken + effectiveFilePath;
        log.debug("{}: file uri: {}", logId(), fileUri);
        return fileUri;
    }

    private String getFileExtension(String filePath) {
        int lastSlashIndex = filePath.lastIndexOf(".");
        return filePath.substring(lastSlashIndex);
    }

    //TODO remove
    private String getMatchLogInfo(Match match) {
        String playerPlaces = match.getMatchPlayers().stream()
                .map(matchPlayer ->
                        String.format("player %d, candidate: %d", matchPlayer.getPlayer().getId(), matchPlayer.getCandidatePlace()))
                .collect(Collectors.joining("; "));
        StringBuilder stringBuilder = new StringBuilder(playerPlaces).append("; ");
        stringBuilder.append("state: ").append(match.getState()).append(", submits: ").append(match.getSubmitsCount());
        return stringBuilder.toString();
    }

    @Override
    public Command getCommand() {
        return Command.UPLOAD_PHOTO;
    }
}
