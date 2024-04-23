package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.ScreenshotFileIOException;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.MatchFinishingService;
import ru.trainithard.dunebot.service.ScreenshotService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.TelegramFileDetailsDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ru.trainithard.dunebot.configuration.SettingConstants.MAX_SCREENSHOT_SIZE;

@SpringBootTest
class PhotoUploadCommandProcessorTest extends TestContextMock {
    private static final String FILE_ID = "fileId123_4%";
    private static final String FILE_URI = "https://api.telegram.org/file/botfake_token/path/file.jpeg";
    private static final String SUCCESSFUL_UPLOAD_TEXT = "Скриншот успешно загружен\\.";
    private static final long FILE_SIZE = 540000;
    private static final long EXTERNAL_USER_ID = 12345L;
    private static final Long CHAT_ID = 100500L;
    private static final Integer REPLY_ID = 9000;
    private static final byte[] MOCK_FILE = "this_file".getBytes();
    private static final TelegramFileDetailsDto fileDetailsDto = new TelegramFileDetailsDto(FILE_ID, "path/file.jpeg", FILE_SIZE);

    @Autowired
    private PhotoUploadCommandProcessor processor;
    @MockBean
    private ScreenshotService screenshotService;
    @MockBean
    private MatchFinishingService matchFinishingService;
    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void beforeEach() {
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + EXTERNAL_USER_ID + ", 9000, 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 12346, 9001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 12347, 9002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 12348, 9003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, submits_count, created_at) " +
                             "values (10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 4, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                             "values (10000, 10000, 10000, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                             "values (10001, 10000, 10001, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                             "values (10002, 10000, 10002, 2, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                             "values (10003, 10000, 10003, 1, '2010-10-10')");
    }

    @AfterEach
    void afterEach() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("photos/11_10"));
        jdbcTemplate.execute("delete from match_players where match_id between 10000 and 10001");
        jdbcTemplate.execute("delete from matches where id between 10000 and 10001");
        jdbcTemplate.execute("delete from players where id between 10000 and 10003");
    }

    @Test
    void shouldRequestForFileWhenCompressedPhotoReceived() {
        processor.process(getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE)));

        verify(restTemplate, times(1)).getForObject(eq(FILE_URI), eq(byte[].class));
    }

    @Test
    void shouldSelectCorrectMatchWhenMultipleMatchesPresent() {
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, submits_count, screenshot_path, created_at) " +
                             "values (10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 4, 'photos/1.jpg', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, candidate_place, created_at) " +
                             "values (10004, 10001, 10000, 4, '2010-10-10')");

        processor.process(getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE)));

        verify(restTemplate, times(1)).getForObject(eq(FILE_URI), eq(byte[].class));
    }

    @ParameterizedTest
    @ValueSource(strings = {".jpg", ".jpeg", ".png"})
    void shouldInvokeScreenshotServiceWhenCompressedPhotoReceived(String extension) throws IOException {
        TelegramFileDetailsDto fileDetails = new TelegramFileDetailsDto(FILE_ID, "path/file" + extension, FILE_SIZE);
        String fileUri = FILE_URI.replace(".jpeg", extension);
        doReturn(MOCK_FILE).when(restTemplate).getForObject(eq(fileUri), eq(byte[].class));
        doReturn(CompletableFuture.completedFuture(fileDetails)).when(messagingService).getFileDetails(FILE_ID);

        processor.process(getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE)));

        verify(screenshotService, times(1)).save(eq(10000L), eq(extension), eq(MOCK_FILE));
    }

    @Test
    void shouldIgnoreFilePathFirstSlashWhenCompressedPhotoReceived() {
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails("/" + FILE_ID);

        processor.process(getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE)));

        verify(restTemplate, times(1)).getForObject(eq(FILE_URI), eq(byte[].class));
    }

    @ParameterizedTest
    @ValueSource(strings = {".jpg", ".jpeg", ".png"})
    void shouldInvokeScreenshotServiceWhenDocumentPhotoReceived(String extension) throws IOException {
        TelegramFileDetailsDto fileDetails = new TelegramFileDetailsDto(FILE_ID, "path/file" + extension, FILE_SIZE);
        doReturn(CompletableFuture.completedFuture(fileDetails)).when(messagingService).getFileDetails(FILE_ID);
        String fileUri = FILE_URI.replace(".jpeg", extension);
        doReturn(MOCK_FILE).when(restTemplate).getForObject(eq(fileUri), eq(byte[].class));

        processor.process(getDocumentCommandMessage());

        verify(screenshotService, times(1)).save(eq(10000L), eq(extension), eq(MOCK_FILE));
    }

    @Test
    void shouldIgnoreFilePathFirstSlashWhenDocumentPhotoReceived() {
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails("/" + FILE_ID);

        processor.process(getDocumentCommandMessage());

        verify(restTemplate, times(1)).getForObject(eq(FILE_URI), eq(byte[].class));
    }

    @Test
    void shouldSelectLargestFileSizeNotExceedingLimitWhenPhotoSizeReceived() {
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID + "XX");
        CommandMessage photoCommandMessage = getPhotoCommandMessage(getPhotos(1, 2, 3));

        processor.process(photoCommandMessage);

        verify(messagingService, times(1)).getFileDetails(eq(FILE_ID + "XX"));
    }

    @Test
    void shouldSendNotificationWhenScreenshotExceptionThrown() throws IOException {
        doThrow(new ScreenshotFileIOException("saving exception")).when(screenshotService).save(anyLong(), any(), any());
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        CommandMessage documentCommandMessage = getDocumentCommandMessage();

        processor.process(documentCommandMessage);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getReplyMessageId, MessageDto::getText)
                .containsExactly(CHAT_ID.toString(), REPLY_ID, "saving exception");
    }

    @Test
    void shouldSendTelegramMessageOnSuccessfulUpload() {
        TelegramFileDetailsDto fileDetails = new TelegramFileDetailsDto(FILE_ID, "path/file.jpeg", FILE_SIZE);
        doReturn(CompletableFuture.completedFuture(fileDetails)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));

        processor.process(getDocumentCommandMessage());

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(CHAT_ID.toString(), SUCCESSFUL_UPLOAD_TEXT);
    }

    @Test
    void shouldSetMatchSubmitPhotoFlagWhenCompressedPhotoReceived() throws IOException {
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("/photos/10_10/10000.jpeg").when(screenshotService).save(eq(10000L), eq(".jpeg"), any());

        processor.process(getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE)));

        Boolean actualSubmitPhotoFlag = jdbcTemplate.queryForObject(
                "select exists (select 1 from matches where id = 10000 and screenshot_path is not null)", Boolean.class);

        assertThat(actualSubmitPhotoFlag).isNotNull().isTrue();
    }

    @Test
    void shouldSetMatchSubmitPhotoFlagWhenDocumentReceived() throws IOException {
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("/photos/10_10/10000.jpeg").when(screenshotService).save(eq(10000L), eq(".jpeg"), any());

        processor.process(getDocumentCommandMessage());

        Boolean actualSubmitPhotoFlag = jdbcTemplate.queryForObject(
                "select exists (select 1 from matches where id = 10000 and screenshot_path is not null)", Boolean.class);

        assertThat(actualSubmitPhotoFlag).isNotNull().isTrue();
    }

    @Test
    void shouldSetSubmitScreenshottedStateWhenCompressedPhotoReceived() throws IOException {
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("/photos/10_10/10000.jpeg").when(screenshotService).save(eq(10000L), eq(".jpeg"), any());

        processor.process(getPhotoCommandMessage(getPhotos(MAX_SCREENSHOT_SIZE)));

        Boolean hasScreenshottedState = jdbcTemplate.queryForObject(
                "select exists (select 1 from matches where id = 10000 and matches.state = '" + MatchState.ON_SUBMIT_SCREENSHOTTED + "')", Boolean.class);

        assertThat(hasScreenshottedState).isNotNull().isTrue();
    }

    @Test
    void shouldSetSubmitScreenshottedStateWhenDocumentReceived() throws IOException {
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("/photos/10_10/10000.jpeg").when(screenshotService).save(eq(10000L), eq(".jpeg"), any());

        processor.process(getDocumentCommandMessage());

        Boolean hasScreenshottedState = jdbcTemplate.queryForObject(
                "select exists (select 1 from matches where id = 10000 and matches.state = '" + MatchState.ON_SUBMIT_SCREENSHOTTED + "')", Boolean.class);

        assertThat(hasScreenshottedState).isNotNull().isTrue();
    }

    @Test
    void shouldNotSetMatchSubmitPhotoFlagWhenScreenshotServiceThrows() throws IOException {
        doThrow(new ScreenshotFileIOException("saving exception")).when(screenshotService).save(anyLong(), any(), any());
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI.replace(".jpeg", ".bmp")), eq(byte[].class));
        CommandMessage documentCommandMessage = getDocumentCommandMessage();

        processor.process(documentCommandMessage);

        Boolean actualSubmitPhotoFlag = jdbcTemplate.queryForObject(
                "select exists (select 1 from matches where id = 10000 and screenshot_path is not null)", Boolean.class);

        assertThat(actualSubmitPhotoFlag).isNotNull().isFalse();
    }

    @Test
    void shouldInvokeMatchFinishOnFileSaveWhenAllPlayersSubmitsAndPhotoPresented() throws IOException {
        TelegramFileDetailsDto fileDetails = new TelegramFileDetailsDto(FILE_ID, "path/file.jpeg", FILE_SIZE);
        doReturn(CompletableFuture.completedFuture(fileDetails)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));
        doReturn("/photos/10_10/10000.jpeg").when(screenshotService).save(eq(10000L), eq(".jpeg"), any());

        processor.process(getDocumentCommandMessage());

        verify(matchFinishingService, times(1)).finishSubmittedMatch(eq(10000L));
    }

    @Test
    void shouldNotInvokeMatchFinishOnFileSaveWhenNotAllPlayersSubmitsPresented() {
        jdbcTemplate.execute("update matches set submits_count = 3 where id = 10000");
        TelegramFileDetailsDto fileDetails = new TelegramFileDetailsDto(FILE_ID, "path/file.jpeg", FILE_SIZE);
        doReturn(CompletableFuture.completedFuture(fileDetails)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));

        processor.process(getDocumentCommandMessage());

        verify(matchFinishingService, never()).finishSubmittedMatch(eq(10000L));
    }

    @Test
    void shouldReturnPhotoUploadCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.UPLOAD_PHOTO);
    }

    private CommandMessage getPhotoCommandMessage(List<PhotoSize> photoSizes) {
        User user = new User();
        user.setId(EXTERNAL_USER_ID);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
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

    private CommandMessage getDocumentCommandMessage() {
        User user = new User();
        user.setId(EXTERNAL_USER_ID);
        Document document = new Document();
        document.setFileId(FILE_ID);
        document.setFileSize((long) MAX_SCREENSHOT_SIZE);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message reply = new Message();
        reply.setMessageId(REPLY_ID);
        Message message = new Message();
        message.setMessageId(100501);
        message.setFrom(user);
        message.setDocument(document);
        message.setChat(chat);
        message.setReplyToMessage(reply);
        return CommandMessage.getMessageInstance(message);
    }
}
