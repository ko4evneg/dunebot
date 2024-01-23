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
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.messaging.dto.TelegramFileDetailsDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class PhotoUploadCommandProcessorTest extends TestContextMock {
    private static final String FILE_ID = "fileId123_4%";
    private static final int MAX_FILE_SIZE = 1_000_000;
    private static final long FILE_SIZE = 540000;
    private static final long EXTERNAL_USER_ID = 12345L;
    private static final String FILE_URI = "https://api.telegram.org/file/botfake_token/path/file.jpeg";
    private static final Path PHOTO_FILE_PATH = Path.of("photos/11_10/10000.jpeg");
    private static final java.time.Instant NOW = LocalDateTime.of(2011, 10, 20, 0, 1).toInstant(ZoneOffset.UTC);
    private static final TelegramFileDetailsDto fileDetailsDto = new TelegramFileDetailsDto(FILE_ID, "path/file.jpeg", FILE_SIZE);
    private static final TelegramFileDetailsDto wrongExtensionFileDetailsDto = new TelegramFileDetailsDto(FILE_ID, "path/file.bmp", FILE_SIZE);
    private static final String SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE = "Ошибка: скриншот уже загружен";
    private static final String WRONG_PHOTO_EXTENSION_EXCEPTION_MESSAGE = "Неподдерживаемое расширение файла. Список поддерживаемых расширений: 'jpg', 'jpeg', 'png'.";
    private static final String SUCCESSFUL_UPLOAD_TEXT = "Скриншот успешно загружен.";
    private static final Long CHAT_ID = 100500L;
    private static final Integer REPLY_ID = 9000;

    @Autowired
    PhotoUploadCommandProcessor processor;
    @MockBean
    private MessagingService messagingService;
    @MockBean
    private RestTemplate restTemplate;
    @MockBean
    private Clock clock;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + EXTERNAL_USER_ID + ", 9000, 'st_pl', 'name', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                "values (10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.ON_SUBMIT + "', 0,  '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                "values (10000, 10000, 10000, '2010-10-10')");

    }

    @AfterEach
    void afterEach() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("photos/11_10"));
        jdbcTemplate.execute("delete from match_players where match_id in (select id from matches where id between 10000 and 10001)");
        jdbcTemplate.execute("delete from matches where id between 10000 and 10001");
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @Test
    void shouldRequestForFileWhenCompressedPhotoReceived() {
        processor.process(getPhotoCommandMessage(getPhotos(MAX_FILE_SIZE)));

        verify(restTemplate, times(1)).getForObject(eq(FILE_URI), eq(byte[].class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jpg", "jpeg", "png"})
    void shouldSaveFileWhenCompressedPhotoReceived(String extension) throws IOException {
        TelegramFileDetailsDto fileDetails = new TelegramFileDetailsDto(FILE_ID, "path/file." + extension, FILE_SIZE);
        Path expectedFilePath = Path.of("photos/11_10/10000." + extension);
        String fileUri = FILE_URI.replace(".jpeg", "." + extension);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(fileUri), eq(byte[].class));
        doReturn(CompletableFuture.completedFuture(fileDetails)).when(messagingService).getFileDetails(FILE_ID);

        processor.process(getPhotoCommandMessage(getPhotos(MAX_FILE_SIZE)));

        String actualFileContent = Files.readString(expectedFilePath);

        assertEquals("this_file", actualFileContent);
    }

    @Test
    void shouldSetMatchSubmitPhotoFlagWhenCompressedPhotoReceived() {
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);

        processor.process(getPhotoCommandMessage(getPhotos(MAX_FILE_SIZE)));

        Boolean actualSubmitPhotoFlag = jdbcTemplate.queryForObject("select has_onsubmit_photo from matches where id = 10000", Boolean.class);

        assertNotNull(actualSubmitPhotoFlag);
        assertTrue(actualSubmitPhotoFlag);
    }

    @Test
    void shouldSetMatchSubmitPhotoFlagWhenDocumentReceived() {
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);

        processor.process(getDocumentCommandMessage());

        Boolean actualSubmitPhotoFlag = jdbcTemplate.queryForObject("select has_onsubmit_photo from matches where id = 10000", Boolean.class);

        assertNotNull(actualSubmitPhotoFlag);
        assertTrue(actualSubmitPhotoFlag);
    }

    @Test
    void shouldIgnoreFilePathFirstSlashWhenCompressedPhotoReceived() {
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails("/" + FILE_ID);

        processor.process(getPhotoCommandMessage(getPhotos(MAX_FILE_SIZE)));

        verify(restTemplate, times(1)).getForObject(eq(FILE_URI), eq(byte[].class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jpg", "jpeg", "png"})
    void shouldSaveFileWhenDocumentPhotoReceived(String extension) throws IOException {
        TelegramFileDetailsDto fileDetails = new TelegramFileDetailsDto(FILE_ID, "path/file." + extension, FILE_SIZE);
        Path expectedFilePath = Path.of("photos/11_10/10000." + extension);
        doReturn(CompletableFuture.completedFuture(fileDetails)).when(messagingService).getFileDetails(FILE_ID);
        String fileUri = FILE_URI.replace(".jpeg", "." + extension);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(fileUri), eq(byte[].class));

        processor.process(getDocumentCommandMessage());

        String actualFileContent = Files.readString(expectedFilePath);

        assertEquals("this_file", actualFileContent);
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
    void shouldSendNotificationWhenPhotoAlreadyExists() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        CommandMessage documentCommandMessage = getDocumentCommandMessage();

        processor.process(documentCommandMessage);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertEquals(REPLY_ID, actualMessage.getReplyMessageId());
        assertEquals(CHAT_ID.toString(), actualMessage.getChatId());
        assertEquals(SCREENSHOT_ALREADY_UPLOADED_EXCEPTION_MESSAGE, actualMessage.getText());
    }

    @Test
    void shouldNotChangeExistingFileWhenPhotoAlreadyExists() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        CommandMessage documentCommandMessage = getDocumentCommandMessage();

        processor.process(documentCommandMessage);

        String actualFileContent = Files.readString(PHOTO_FILE_PATH);
        assertEquals("hehe", actualFileContent);
    }

    @Test
    void shouldSendNotificationWhenFileExtensionNotAllowed() {
        doReturn(CompletableFuture.completedFuture(wrongExtensionFileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI.replace(".jpeg", ".bmp")), eq(byte[].class));
        CommandMessage documentCommandMessage = getDocumentCommandMessage();

        processor.process(documentCommandMessage);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertEquals(REPLY_ID, actualMessage.getReplyMessageId());
        assertEquals(CHAT_ID.toString(), actualMessage.getChatId());
        assertEquals(WRONG_PHOTO_EXTENSION_EXCEPTION_MESSAGE, actualMessage.getText());
    }

    @Test
    void shouldNotSetMatchSubmitPhotoFlagWhenFileExtensionNotAllowed() {
        doReturn(CompletableFuture.completedFuture(wrongExtensionFileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI.replace(".jpeg", ".bmp")), eq(byte[].class));
        CommandMessage documentCommandMessage = getDocumentCommandMessage();

        processor.process(documentCommandMessage);

        Boolean actualSubmitPhotoFlag = jdbcTemplate.queryForObject("select has_onsubmit_photo from matches where id = 10000", Boolean.class);

        assertNotNull(actualSubmitPhotoFlag);
        assertFalse(actualSubmitPhotoFlag);
    }

    @Test
    void shouldNotChangeExistingFileWhenFileExtensionNotAllowed() throws IOException {
        Files.createDirectories(PHOTO_FILE_PATH.getParent());
        Files.write(PHOTO_FILE_PATH, "hehe".getBytes());
        doReturn(CompletableFuture.completedFuture(wrongExtensionFileDetailsDto)).when(messagingService).getFileDetails(FILE_ID);
        CommandMessage documentCommandMessage = getDocumentCommandMessage();

        processor.process(documentCommandMessage);

        String actualFileContent = Files.readString(PHOTO_FILE_PATH);
        assertEquals("hehe", actualFileContent);
    }

    @Test
    void shouldSendTelegramMessageOnSuccessfulUpload() {
        TelegramFileDetailsDto fileDetails = new TelegramFileDetailsDto(FILE_ID, "path/file.jpeg", FILE_SIZE);
        doReturn(CompletableFuture.completedFuture(fileDetails)).when(messagingService).getFileDetails(FILE_ID);
        doReturn("this_file".getBytes()).when(restTemplate).getForObject(eq(FILE_URI), eq(byte[].class));

        processor.process(getDocumentCommandMessage());

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessages = messageCaptor.getValue();

        assertEquals(CHAT_ID.toString(), actualMessages.getChatId());
        assertEquals(SUCCESSFUL_UPLOAD_TEXT, actualMessages.getText());
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
        document.setFileSize((long) MAX_FILE_SIZE);
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
