package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.FileSystemUtils;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.ScreenshotFileIOException;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
class ScreenshotServiceTest extends TestContextMock {
    private static final Path PHOTO_FILE_PATH = Path.of("photos/11_10/10000.jpeg");
    private static final long EXTERNAL_USER_ID = 12345L;
    private static final String SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE = "Ошибка: скриншот уже загружен";
    private static final String WRONG_PHOTO_EXTENSION_EXCEPTION_MESSAGE = "Неподдерживаемое расширение файла. Список поддерживаемых расширений: 'jpg', 'jpeg', 'png'.";
    private static final java.time.Instant NOW = LocalDateTime.of(2011, 10, 20, 0, 1).toInstant(ZoneOffset.UTC);
    private static final String FILE_EXTENSION = ".jpeg";

    @Autowired
    private ScreenshotService screenshotService;
    @MockBean
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + EXTERNAL_USER_ID + ", 9000, 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, submits_count, created_at) " +
                             "values (10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 4, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                             "values (10000, 10000, 10000, 4, '2010-10-10')");
    }

    @AfterEach
    void afterEach() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("photos"));
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id between 10000 and 10003");
    }

    @ParameterizedTest
    @ValueSource(strings = {".jpg", FILE_EXTENSION, ".png"})
    void shouldSaveFileWithCorrectExtension(String extension) throws IOException {
        screenshotService.save(10000L, extension, "this_file".getBytes());

        String actualFileContent = Files.readString(Path.of("photos/11_10/10000" + extension));

        assertThat(actualFileContent).isEqualTo("this_file");
    }

    @Test
    void shouldThrowWhenFileExtensionNotAllowed() {
        fail();
        byte[] file = "this_file".getBytes();
        assertThatThrownBy(() -> screenshotService.save(10000L, ".bmp", file))
                .isInstanceOf(ScreenshotFileIOException.class)
                .hasMessage(WRONG_PHOTO_EXTENSION_EXCEPTION_MESSAGE);
    }

    @Test
    void shouldNotSaveFileWhenFileExtensionNotAllowed() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());

        try {
            screenshotService.save(10000L, ".bmp", "this_file".getBytes());
        } catch (Exception ignored) {
        }

        String actualFileContent = Files.readString(Path.of("photos/11_10/10000.jpeg"));

        assertThat(actualFileContent).isEqualTo("hehe");
    }

    @Test
    void shouldThrowWhenPhotoAlreadyExists() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());

        byte[] file = "this_file".getBytes();

        assertThatThrownBy(() -> screenshotService.save(10000L, FILE_EXTENSION, file))
                .isInstanceOf(ScreenshotFileIOException.class)
                .hasMessage(SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE);
    }

    @Test
    void shouldNotReplaceFileWhenPhotoAlreadyExists() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());

        try {
            screenshotService.save(10000L, FILE_EXTENSION, "this_file".getBytes());
        } catch (Exception ignored) {
        }

        String actualFileContent = Files.readString(Path.of("photos/11_10/10000.jpeg"));

        assertThat(actualFileContent).isEqualTo("hehe");
    }
}
