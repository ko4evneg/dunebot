package ru.trainithard.dunebot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.exception.ScreenshotFileIOException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;

import static ru.trainithard.dunebot.configuration.SettingConstants.PHOTO_ALLOWED_EXTENSIONS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotServiceImpl implements ScreenshotService {
    private static final String SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE = "Ошибка: скриншот уже загружен";
    private static final String WRONG_PHOTO_EXTENSION_EXCEPTION_MESSAGE = "Неподдерживаемое расширение файла. Список поддерживаемых расширений: 'jpg', 'jpeg', 'png'.";
    private static final String FILE_PATH_SEPARATOR = "/";

    private final Clock clock;

    @Value("${bot.photos-directory}")
    private String photosDirectoryPath;

    @Override
    public String save(long matchId, String dottedFileExtension, byte[] screenshot) throws IOException {
        int logId = LogId.get();
        log.debug("{}: saving file", logId);
        LocalDate today = LocalDateTime.ofInstant(Instant.now(clock), ZoneId.systemDefault()).toLocalDate();
        String monthYear = today.format(DateTimeFormatter.ofPattern("yy_MM"));
        Path savePath = Path.of(getSaveDirectoryPath(monthYear) + matchId + dottedFileExtension);
        log.debug("{}: save path: '{}'", logId, savePath);

        validate(dottedFileExtension, savePath);

        Files.createDirectories(savePath.getParent());
        Files.write(savePath, screenshot);

        log.debug("{}: file successfully saved", logId);
        return savePath.toString();
    }

    private void validate(String dottedFileExtension, Path savePath) {
        boolean hasValidExtension = !PHOTO_ALLOWED_EXTENSIONS.contains(dottedFileExtension);
        if (hasValidExtension) {
            throw new ScreenshotFileIOException(WRONG_PHOTO_EXTENSION_EXCEPTION_MESSAGE);
        }
        if (Files.exists(savePath)) {
            throw new ScreenshotFileIOException(SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE);
        }
    }

    private String getSaveDirectoryPath(String monthYear) {
        return photosDirectoryPath + FILE_PATH_SEPARATOR + monthYear + FILE_PATH_SEPARATOR;
    }
}
