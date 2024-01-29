package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.exception.ScreenshotSavingException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.ScreenshotService;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.TelegramFileDetailsDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import static ru.trainithard.dunebot.configuration.SettingConstants.MAX_SCREENSHOT_SIZE;

@Service
@RequiredArgsConstructor
public class PhotoUploadCommandProcessor extends CommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PhotoUploadCommandProcessor.class);
    private static final String FILE_DOWNLOAD_URI_PREFIX = "https://api.telegram.org/file/bot";
    private static final String PATH_SEPARATOR = "/";
    private static final String UNSUPPORTED_UPDATE_TYPE = "Unsupported photo/document update type";
    private static final String SUCCESSFUL_UPLOAD_TEXT = "Скриншот успешно загружен.";

    private final MessagingService messagingService;
    private final RestTemplate restTemplate;
    private final MatchRepository matchRepository;
    private final ScreenshotService screenshotService;
    private final MatchFinishingService matchFinishingService;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public void process(CommandMessage commandMessage, int loggingId) {
        logger.debug("{}: new started", loggingId);

        Match match = matchRepository.findLatestPlayerMatchWithMatchPlayerBy(commandMessage.getUserId(), MatchState.ON_SUBMIT).iterator().next();
        logger.debug("{}: match found, id: {}", loggingId, match.getId());

        String fileId = getFileId(commandMessage);
        CompletableFuture<TelegramFileDetailsDto> file = messagingService.getFileDetails(fileId);
        file.whenComplete((telegramFileDetailsDto, throwable) -> {
            logger.debug("{}: file received from telegram", loggingId);

            String filePath = telegramFileDetailsDto.path();
            String effectiveFilePath = filePath.startsWith(PATH_SEPARATOR) ? filePath : PATH_SEPARATOR + filePath;
            byte[] photoBytes = restTemplate.getForObject(getFileUri(effectiveFilePath), byte[].class);
            logger.debug("{}: file bytes array received from telegram", loggingId);

            try {
                if (photoBytes != null) {
                    String dottedFileExtension = getFileExtension(filePath);
                    screenshotService.save(match.getId(), dottedFileExtension, photoBytes);
                    match.setHasSubmitPhoto(true);
                    matchRepository.save(match);
                    logger.debug("{}: match finish conditions checking", loggingId);
                    if (match.canBeFinished()) {
                        matchFinishingService.finishSuccessfullySubmittedMatch(match.getId(), loggingId);
                    }

                    messagingService.sendMessageAsync(new MessageDto(commandMessage, SUCCESSFUL_UPLOAD_TEXT, null));
                }
            } catch (ScreenshotSavingException exception) {
                logger.error(loggingId + ": screenshot save failed due to an exception", exception);
                messagingService.sendMessageAsync(new MessageDto(commandMessage, exception.getMessage(), null));
            } catch (IOException exception) {
                logger.error(loggingId + ": encountered an exception", exception);
            }
        });

        logger.debug("{}: new ended", loggingId);
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
        return FILE_DOWNLOAD_URI_PREFIX + botToken + filePath;
    }

    private String getFileExtension(String filePath) {
        int lastSlashIndex = filePath.lastIndexOf(".");
        return filePath.substring(lastSlashIndex);
    }

    @Override
    public Command getCommand() {
        return Command.UPLOAD_PHOTO;
    }
}
