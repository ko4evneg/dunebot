package ru.trainithard.dunebot.service.telegram.command.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.TelegramFileDetailsDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static ru.trainithard.dunebot.configuration.SettingConstants.MAX_FILE_SIZE;

@Service
@RequiredArgsConstructor
public class PhotoUploadCommandProcessor extends CommandProcessor {
    private static final String FILE_DOWNLOAD_URI_PREFIX = "https://api.telegram.org/file/bot";
    private static final String FILE_SIZE_LIMIT_EXCEPTION_MESSAGE_TEMPLATE = "Файл слишком большой. Разрешенный максимальный размер: %s КБ";
    private static final String PATH_SEPARATOR = "/";

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
        String fileId = getValidatedSizeFileId(commandMessage);
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
                    Files.createDirectories(savePath.getParent());
                    Files.write(savePath, photoBytes);
                }
            } catch (IOException ignored) {
                // TODO: log?
            }
        });
    }

    private String getValidatedSizeFileId(CommandMessage commandMessage) {
        if (commandMessage.getPhoto() != null) {
            List<CommandMessage.Photo> toSortPhotos = new ArrayList<>(commandMessage.getPhoto());
            toSortPhotos.sort(Comparator.comparing(CommandMessage.Photo::size).reversed());
            for (CommandMessage.Photo photo : toSortPhotos) {
                if (photo.size() <= MAX_FILE_SIZE) {
                    return photo.id();
                }
            }
        } else if (commandMessage.getFile() != null && commandMessage.getFile().size() <= MAX_FILE_SIZE) {
            return commandMessage.getFile().id();
        }

        throw new AnswerableDuneBotException(getFileSizeLimitExceptionMessage(), commandMessage);
    }

    private String getFileSizeLimitExceptionMessage() {
        int effectiveMaxFileSize = MAX_FILE_SIZE > 1000 ? MAX_FILE_SIZE / 1000 : MAX_FILE_SIZE;
        return String.format(FILE_SIZE_LIMIT_EXCEPTION_MESSAGE_TEMPLATE, effectiveMaxFileSize);
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
