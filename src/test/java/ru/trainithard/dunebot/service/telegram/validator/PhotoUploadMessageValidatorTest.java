package ru.trainithard.dunebot.service.telegram.validator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.TelegramFileDetailsDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.trainithard.dunebot.configuration.SettingConstants.MAX_SCREENSHOT_SIZE;

@SpringBootTest
class PhotoUploadMessageValidatorTest extends TestContextMock {
    private static final long EXTERNAL_USER_ID = 12345L;
    private static final String FILE_URI = "https://api.telegram.org/file/botfake_token/path/file.jpeg";
    private static final String FILE_ID = "fileId123_4%";
    private static final long FILE_SIZE = 540000;
    private static final String FILE_SIZE_LIMIT_EXCEPTION_MESSAGE = "Файл дюже большой. Разрешенный максимальный размер: 6500 КБ";
    private static final String MULTIPLE_ONSUBMIT_MATCHES_EXCEPTION_MESSAGE =
            "У вас более одного матча (10000, 10001) в процессе регистрации результата. Выйдите из неактуальных опросов и загрузите скриншот вновь.";
    private static final String NOT_SUBMITTED_MATCH_EXCEPTION_MESSAGE =
            "У вас нет матчей в процессе регистрации результата. Для запуска регистрации выполните команду: '/submit *ID матча*'";
    private static final String SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE = "Скриншот уже загружен";
    private static final TelegramFileDetailsDto fileDetailsDto = new TelegramFileDetailsDto(FILE_ID, "path/file.jpeg", FILE_SIZE);

    @Autowired
    private PhotoUploadMessageValidator validator;
    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void beforeEach() {
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + EXTERNAL_USER_ID + ", 9000, 'st_pl', 'name', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state,positive_answers_count, created_at) " +
                             "values (10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 0, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10000, 10000, 10000, '2010-10-10')");

    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id in (select id from matches where id between 10000 and 10001)");
        jdbcTemplate.execute("delete from matches where id between 10000 and 10001");
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"ON_SUBMIT_SCREENSHOTTED", "ON_SUBMIT"})
    void shouldThrowWhenSavingNotOnSubmitMatchOnPhotoSizeUpload(MatchState state) {
        jdbcTemplate.execute("update matches set state = '" + state + "' where id = 10000");
        CommandMessage photoCommandMessage = getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE));

        AnswerableDuneBotException actualException =
                assertThrows(AnswerableDuneBotException.class, () -> validator.validate(photoCommandMessage));
        assertEquals(NOT_SUBMITTED_MATCH_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"ON_SUBMIT_SCREENSHOTTED", "ON_SUBMIT"})
    void shouldThrowWhenSavingNotOnSubmitMatchOnDocumentUpload(MatchState state) {
        jdbcTemplate.execute("update matches set state = '" + state + "' where id = 10000");
        CommandMessage documentCommandMessage = getDocumentCommandMessage(MAX_SCREENSHOT_SIZE);

        AnswerableDuneBotException actualException =
                assertThrows(AnswerableDuneBotException.class, () -> validator.validate(documentCommandMessage));
        assertEquals(NOT_SUBMITTED_MATCH_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldThrowWhenScreenshotAlreadyUploadedOnPhotoSizeUpload() {
        jdbcTemplate.execute("update matches set state = '" + MatchState.ON_SUBMIT_SCREENSHOTTED + "' where id = 10000");
        CommandMessage photoCommandMessage = getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE));

        AnswerableDuneBotException actualException =
                assertThrows(AnswerableDuneBotException.class, () -> validator.validate(photoCommandMessage));
        assertEquals(SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldThrowWhenScreenshotAlreadyUploadedOnDocumentUpload() {
        jdbcTemplate.execute("update matches set state = '" + MatchState.ON_SUBMIT_SCREENSHOTTED + "' where id = 10000");
        CommandMessage documentCommandMessage = getDocumentCommandMessage(MAX_SCREENSHOT_SIZE);

        AnswerableDuneBotException actualException =
                assertThrows(AnswerableDuneBotException.class, () -> validator.validate(documentCommandMessage));
        assertEquals(SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldThrowWhenMultipleOnSubmitMatchesExistOnPhotoSizeUpload() {
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 0,  '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 10001, 10000, '2010-10-10')");
        CommandMessage photoCommandMessage = getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE));

        AnswerableDuneBotException actualException =
                assertThrows(AnswerableDuneBotException.class, () -> validator.validate(photoCommandMessage));
        assertEquals(MULTIPLE_ONSUBMIT_MATCHES_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldThrowWhenMultipleOnSubmitMatchesExistOnDocumentUpload() {
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 0, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 10001, 10000, '2010-10-10')");
        CommandMessage documentCommandMessage = getDocumentCommandMessage(MAX_SCREENSHOT_SIZE);

        AnswerableDuneBotException actualException =
                assertThrows(AnswerableDuneBotException.class, () -> validator.validate(documentCommandMessage));
        assertEquals(MULTIPLE_ONSUBMIT_MATCHES_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldThrowOnFileSizeExceedsLimitWhenDocumentPhotoReceived() {
        CommandMessage documentCommandMessage = getDocumentCommandMessage(MAX_SCREENSHOT_SIZE + 1);

        AnswerableDuneBotException actualException =
                assertThrows(AnswerableDuneBotException.class, () -> validator.validate(documentCommandMessage));
        assertEquals(FILE_SIZE_LIMIT_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldThrowOnEveryFileSizeExceedsLimitWhenPhotoSizeReceived() {
        CommandMessage photoCommandMessage = getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE + 1, MAX_SCREENSHOT_SIZE + 2));

        AnswerableDuneBotException actualException =
                assertThrows(AnswerableDuneBotException.class, () -> validator.validate(photoCommandMessage));
        assertEquals(FILE_SIZE_LIMIT_EXCEPTION_MESSAGE, actualException.getMessage());
    }

    @Test
    void shouldNotThrowOnSingleFileSizeExceedsLimitWhenPhotoSizeReceived() {
        CommandMessage photoCommandMessage = getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE, MAX_SCREENSHOT_SIZE + 1));

        assertDoesNotThrow(() -> validator.validate(photoCommandMessage));
    }

    private CommandMessage getPhotoCommandMessage(List<PhotoSize> photoSizes) {
        User user = new User();
        user.setId(EXTERNAL_USER_ID);
        Chat chat = new Chat();
        chat.setId(100500L);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(100501);
        message.setFrom(user);
        message.setPhoto(photoSizes);
        message.setChat(chat);
        return CommandMessage.getMessageInstance(message);
    }

    private List<PhotoSize> getPhotos(int... sizes) {
        List<PhotoSize> photoSizes = new ArrayList<>();
        StringBuilder fileIdIncrement = new StringBuilder(FILE_ID);
        for (int size : sizes) {
            PhotoSize photoSize = new PhotoSize();
            photoSize.setFileId(fileIdIncrement.toString());
            photoSize.setFileSize(size);
            photoSizes.add(photoSize);
            fileIdIncrement.append("X");
        }
        return photoSizes;
    }

    private CommandMessage getDocumentCommandMessage(int fileSize) {
        User user = new User();
        user.setId(EXTERNAL_USER_ID);
        Document document = new Document();
        document.setFileId(FILE_ID);
        document.setFileSize((long) fileSize);
        Chat chat = new Chat();
        chat.setId(100500L);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(100501);
        message.setFrom(user);
        message.setDocument(document);
        message.setChat(chat);
        return CommandMessage.getMessageInstance(message);
    }
}
