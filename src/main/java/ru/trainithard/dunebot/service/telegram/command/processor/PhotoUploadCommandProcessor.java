package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.TelegramFileDetailsDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import static ru.trainithard.dunebot.configuration.SettingConstants.MAX_FILE_SIZE;

@Service
@RequiredArgsConstructor
public class PhotoUploadCommandProcessor extends CommandProcessor {
    private static final String FILE_DOWNLOAD_URI_PREFIX = "https://api.telegram.org/file/bot";
    private static final String PATH_SEPARATOR = "/";
    private static final String UNSUPPORTED_UPDATE_TYPE = "Unsupported photo/document update type";
    private static final String SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE = "Скриншот уже загружен";

    private final MessagingService messagingService;
    private final RestTemplate restTemplate;
    private final MatchRepository matchRepository;
    private final Clock clock;
    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.photos-directory}")
    private String photosDirectoryPath;

    @Override
    public void process(CommandMessage commandMessage) {
        Match match = matchRepository.findLatestPlayerOnSubmitMatch(commandMessage.getUserId()).iterator().next();
        String fileId = getFileId(commandMessage);
        CompletableFuture<TelegramFileDetailsDto> file = messagingService.getFileDetails(fileId);
        file.whenComplete((telegramFileDetailsDto, throwable) -> {
            String filePath = telegramFileDetailsDto.path();
            String effectiveFilePath = filePath.startsWith(PATH_SEPARATOR) ? filePath : PATH_SEPARATOR + filePath;
            byte[] photoBytes = restTemplate.getForObject(getFileUri(effectiveFilePath), byte[].class);
            try {
                if (photoBytes != null) {
                    LocalDate today = LocalDateTime.ofInstant(Instant.now(clock), ZoneId.systemDefault()).toLocalDate();
                    String monthYear = today.format(DateTimeFormatter.ofPattern("yy_MM"));
                    Path savePath = Path.of(getSaveDirectoryPath(monthYear) + getSaveFileName(match.getId(), filePath));
                    validateIfExists(savePath, commandMessage);
                    Files.createDirectories(savePath.getParent());
                    Files.write(savePath, photoBytes);
                }
            } catch (IOException ignored) {
                // TODO: log?
            }
        });
    }

    private void validateIfExists(Path savePath, CommandMessage commandMessage) {
        if (Files.exists(savePath)) {
            MessageDto messageDto = new MessageDto(Long.toString(commandMessage.getChatId()), SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE, commandMessage.getReplyMessageId(), null);
            messagingService.sendMessageAsync(messageDto);
            throw new DuneBotException(SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE);
        }
    }

    private String getFileId(CommandMessage commandMessage) {
        if (commandMessage.getPhoto() != null) {
            CommandMessage.Photo largestPhoto = commandMessage.getPhoto().stream()
                    .filter(photo -> photo.size() <= MAX_FILE_SIZE)
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

    private String getSaveDirectoryPath(String monthYear) {
        return photosDirectoryPath + PATH_SEPARATOR + monthYear + PATH_SEPARATOR;
    }

    private String getSaveFileName(long matchId, String filePath) {
        int lastSlashIndex = filePath.lastIndexOf(".");
        String fileExtension = filePath.substring(lastSlashIndex);
        return matchId + fileExtension;
    }

    @Override
    public Command getCommand() {
        return Command.UPLOAD_PHOTO;
    }
}
