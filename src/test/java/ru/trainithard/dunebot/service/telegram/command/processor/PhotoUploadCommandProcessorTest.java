package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.MessagingService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
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
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, positive_answers_count, is_onsubmit, created_at) " +
                "values (10000, 10000, '" + ModType.CLASSIC + "', 0, true, '2010-10-10') ");
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

    @Test
    void shouldSaveFileWhenCompressedPhotoReceived() throws IOException {
        processor.process(getPhotoCommandMessage(getPhotos(MAX_FILE_SIZE)));

        String actualFileContent = Files.readString(PHOTO_FILE_PATH);

        assertEquals("this_file", actualFileContent);
    }

    @Test
    void shouldIgnoreFilePathFirstSlashWhenCompressedPhotoReceived() {
        doReturn(CompletableFuture.completedFuture(fileDetailsDto)).when(messagingService).getFileDetails("/" + FILE_ID);

        processor.process(getPhotoCommandMessage(getPhotos(MAX_FILE_SIZE)));

        verify(restTemplate, times(1)).getForObject(eq(FILE_URI), eq(byte[].class));
    }

    @Test
    void shouldRequestForFileWhenDocumentPhotoReceived() {
        processor.process(getDocumentCommandMessage());

        verify(restTemplate, times(1)).getForObject(eq(FILE_URI), eq(byte[].class));
    }

    @Test
    void shouldSaveFileWhenDocumentPhotoReceived() throws IOException {
        processor.process(getDocumentCommandMessage());

        String actualFileContent = Files.readString(PHOTO_FILE_PATH);

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
    void shouldThrowIfPhotoAlreadyExists() {
        fail();
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

    private CommandMessage getDocumentCommandMessage() {
        User user = new User();
        user.setId(EXTERNAL_USER_ID);
        Document document = new Document();
        document.setFileId(FILE_ID);
        document.setFileSize((long) MAX_FILE_SIZE);
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
