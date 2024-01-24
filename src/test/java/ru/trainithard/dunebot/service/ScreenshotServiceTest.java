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
import ru.trainithard.dunebot.exception.ScreenshotSavingException;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.repository.MatchRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
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
    @Autowired
    private MatchRepository matchRepository;
    @MockBean
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + EXTERNAL_USER_ID + ", 9000, 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, submits_count, has_onsubmit_photo, created_at) " +
                "values (10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 4, false, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                "values (10000, 10000, 10000, 4, '2010-10-10')");
    }

    @AfterEach
    void afterEach() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("photos/11_10"));
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id between 10000 and 10003");
    }

    @ParameterizedTest
    @ValueSource(strings = {".jpg", FILE_EXTENSION, ".png"})
    void shouldSaveFileWithCorrectExtension(String extension) throws IOException {
        Match match = matchRepository.findById(10000L).orElseThrow();

        screenshotService.save(match, extension, "this_file".getBytes());

        String actualFileContent = Files.readString(Path.of("photos/11_10/10000" + extension));

        assertEquals("this_file", actualFileContent);
    }

    @Test
    void shouldSetMatchSubmitPhotoFlagOnSave() throws IOException {
        Match match = matchRepository.findById(10000L).orElseThrow();

        screenshotService.save(match, FILE_EXTENSION, "this_file".getBytes());

        Boolean actualSubmitPhotoFlag = jdbcTemplate.queryForObject("select has_onsubmit_photo from matches where id = 10000", Boolean.class);

        assertNotNull(actualSubmitPhotoFlag);
        assertTrue(actualSubmitPhotoFlag);
    }

    @Test
    void shouldThrowWhenFileExtensionNotAllowed() {
        Match match = matchRepository.findById(10000L).orElseThrow();

        byte[] file = "this_file".getBytes();
        ScreenshotSavingException actualException = assertThrows(ScreenshotSavingException.class,
                () -> screenshotService.save(match, ".bmp", file));
        assertEquals(WRONG_PHOTO_EXTENSION_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldNotSaveFileWhenFileExtensionNotAllowed() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());
        Match match = matchRepository.findById(10000L).orElseThrow();

        try {
            screenshotService.save(match, ".bmp", "this_file".getBytes());
        } catch (Exception ignored) {
        }

        String actualFileContent = Files.readString(Path.of("photos/11_10/10000.jpeg"));

        assertEquals("hehe", actualFileContent);
    }

    @Test
    void shouldNotSetMatchPhotoFlagWhenFileExtensionNotAllowed() {
        Match match = matchRepository.findById(10000L).orElseThrow();

        try {
            screenshotService.save(match, ".bmp", "this_file".getBytes());
        } catch (Exception ignored) {
        }

        Boolean actualSubmitPhotoFlag = jdbcTemplate.queryForObject("select has_onsubmit_photo from matches where id = 10000", Boolean.class);

        assertNotNull(actualSubmitPhotoFlag);
        assertFalse(actualSubmitPhotoFlag);
    }

    @Test
    void shouldThrowWhenPhotoAlreadyExists() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());
        Match match = matchRepository.findById(10000L).orElseThrow();

        byte[] file = "this_file".getBytes();
        ScreenshotSavingException actualException = assertThrows(ScreenshotSavingException.class,
                () -> screenshotService.save(match, FILE_EXTENSION, file));
        assertEquals(SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldNotReplaceFileWhenPhotoAlreadyExists() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());
        Match match = matchRepository.findById(10000L).orElseThrow();

        try {
            screenshotService.save(match, FILE_EXTENSION, "this_file".getBytes());
        } catch (Exception ignored) {
        }

        String actualFileContent = Files.readString(Path.of("photos/11_10/10000.jpeg"));

        assertEquals("hehe", actualFileContent);
    }
}
