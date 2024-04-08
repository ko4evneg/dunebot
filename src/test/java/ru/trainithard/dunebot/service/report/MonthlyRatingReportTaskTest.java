package ru.trainithard.dunebot.service.report;

import com.itextpdf.text.pdf.PdfReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.SettingKey;
import ru.trainithard.dunebot.service.SettingsService;
import ru.trainithard.dunebot.service.messaging.dto.FileMessageDto;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class MonthlyRatingReportTaskTest extends TestContextMock {
    private static final String MATCH_CHAT_ID = "12345";
    private static final int MATCH_TOPIC_REPLY_ID_1 = 9000;
    private static final int MATCH_TOPIC_REPLY_ID_2 = 9001;

    @Autowired
    private MonthlyRatingReportTask task;
    @SpyBean
    private RatingReportPdfService ratingReportPdfService;
    @MockBean
    private SettingsService settingsService;
    @MockBean
    private Clock clock;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void beforeEach() {
        doReturn(ZoneId.of("UTC+3")).when(clock).getZone();
        doReturn(TestConstants.CHAT_ID).when(settingsService).getStringSetting(eq(SettingKey.CHAT_ID));
        doReturn(TestConstants.TOPIC_ID_UPRISING).when(settingsService).getIntSetting(eq(SettingKey.TOPIC_ID_UPRISING));
        doReturn(TestConstants.TOPIC_ID_CLASSIC).when(settingsService).getIntSetting(eq(SettingKey.TOPIC_ID_CLASSIC));

        try {
            Field field = MonthlyRatingReportTask.class.getDeclaredField("pdfPath");
            field.setAccessible(true);
            field.set(task, tempDir.toString());
        } catch (ReflectiveOperationException exception) {
            fail();
        }

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, 11000, 12000, 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 11001, 12001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 12002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 12003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10004, 11004, 12004, 'st_pl5', 'name5', 'l5', 'e5', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10005, 11005, 12005, 'st_pl6', 'name6', 'l6', 'e6', '2010-10-10') ");

        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, " + MATCH_CHAT_ID + ", '10000', " + MATCH_TOPIC_REPLY_ID_1 + ", '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10001, 'ExternalPollId', 10001, " + MATCH_CHAT_ID + ", '10001', " + MATCH_TOPIC_REPLY_ID_2 + ", '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10002, 'ExternalPollId', 10002, " + MATCH_CHAT_ID + ", '10002', " + MATCH_TOPIC_REPLY_ID_2 + ", '2020-10-10')");

        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, finish_date, created_at) " +
                             "values (15000, 10000, 10000, '" + ModType.UPRISING_4 + "', '" + MatchState.FINISHED + "', '2010-10-01', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, finish_date, created_at) " +
                             "values (15001, 10001, 10000, '" + ModType.UPRISING_4 + "', '" + MatchState.FINISHED + "', '2010-10-10', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, finish_date, created_at) " +
                             "values (15002, 10002, 10000, '" + ModType.UPRISING_4 + "', '" + MatchState.FINISHED + "', '2010-10-30', '2010-10-10') ");

        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10000, 15000, 10000, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10001, 15000, 10001, 2, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10002, 15000, 10002, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10003, 15000, 10003, 1, '2010-10-10')");

        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10005, 15001, 10000, 1, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10006, 15001, 10001, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10007, 15001, 10002, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10008, 15001, 10004, 2, '2010-10-10')");

        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10009, 15002, 10001, 1, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10010, 15002, 10002, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10011, 15002, 10003, 0, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10012, 15002, 10004, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10013, 15002, 10005, 2, '2010-10-10')");

        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10000, '" + SettingKey.MONTHLY_MATCHES_THRESHOLD + "', 15, '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10001, '" + SettingKey.CHAT_ID + "', '" + TestConstants.CHAT_ID + "', '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10002, '" + SettingKey.TOPIC_ID_CLASSIC + "', '" + MATCH_TOPIC_REPLY_ID_1 + "', '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10003, '" + SettingKey.TOPIC_ID_UPRISING + "', '" + TestConstants.TOPIC_ID_UPRISING + "', '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from settings where id between 10000 and 10003");
        jdbcTemplate.execute("delete from match_players where match_id between 15000 and 15003");
        jdbcTemplate.execute("delete from matches where id between 15000 and 15003");
        jdbcTemplate.execute("delete from players where id between 10000 and 10005");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10003");
    }

    @Test
    void shouldSaveCorrectReport() throws IOException {
        LocalDateTime firstDay = LocalDate.of(2010, 11, 1).atTime(15, 0);
        Clock fixedClock = Clock.fixed(firstDay.toInstant(ZoneOffset.of("+03:00")), ZoneOffset.of("+03:00"));
        doReturn(fixedClock.instant()).when(clock).instant();

        task.run();

        InputStream actualFileStream = new BufferedInputStream(new FileInputStream(tempDir.resolve("2010_OCTOBER.pdf").toString()));
        PdfReader pdfReader = new PdfReader(actualFileStream);
        PdfReader referencePdfReader = new PdfReader("src/test/resources/pdf/monthly_rate_example_2.pdf");
        byte[] actualBytes = pdfReader.getPageContent(1);
        byte[] expectedBytes = referencePdfReader.getPageContent(1);

        pdfReader.close();
        actualFileStream.close();
        referencePdfReader.close();
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void shouldNotConsiderAnotherModTypesForReport() throws IOException {
        LocalDateTime firstDay = LocalDate.of(2010, 11, 1).atTime(15, 0);
        Clock fixedClock = Clock.fixed(firstDay.toInstant(ZoneOffset.of("+03:00")), ZoneOffset.of("+03:00"));
        doReturn(fixedClock.instant()).when(clock).instant();

        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10003, 'ExternalPollId', 10003, " + MATCH_CHAT_ID + ", '10003', " + MATCH_TOPIC_REPLY_ID_1 + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, finish_date, created_at) " +
                             "values (15003, 10003, 10000, '" + ModType.CLASSIC + "', '" + MatchState.FINISHED + "', '2010-10-30', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10014, 15003, 10000, 1, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10015, 15003, 10001, 4, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10016, 15003, 10002, 3, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, created_at) " +
                             "values (10017, 15003, 10004, 2, '2010-10-10')");
        task.run();

        InputStream actualFileStream = new BufferedInputStream(new FileInputStream(tempDir.resolve("2010_OCTOBER.pdf").toString()));
        PdfReader pdfReader = new PdfReader(actualFileStream);
        PdfReader referencePdfReader = new PdfReader("src/test/resources/pdf/monthly_rate_example_2.pdf");
        byte[] actualBytes = pdfReader.getPageContent(1);
        byte[] expectedBytes = referencePdfReader.getPageContent(1);

        pdfReader.close();
        actualFileStream.close();
        referencePdfReader.close();
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void shouldSendReportToModTypeChat() throws IOException {
        LocalDateTime firstDay = LocalDate.of(2010, 11, 1).atTime(15, 0);
        Clock fixedClock = Clock.fixed(firstDay.toInstant(ZoneOffset.of("+03:00")), ZoneOffset.of("+03:00"));
        doReturn(fixedClock.instant()).when(clock).instant();

        task.run();

        InputStream actualFileStream = new BufferedInputStream(new FileInputStream(tempDir.resolve("2010_OCTOBER.pdf").toString()));
        PdfReader referencePdfReader = new PdfReader("src/test/resources/pdf/monthly_rate_example_2.pdf");
        byte[] referenceFileBytes = referencePdfReader.getPageContent(1);
        referencePdfReader.close();
        actualFileStream.close();

        ArgumentCaptor<FileMessageDto> messageCaptor = ArgumentCaptor.forClass(FileMessageDto.class);
        verify(messagingService, times(2)).sendFileAsync(messageCaptor.capture());

        FileMessageDto actualUprisingMessage = messageCaptor.getAllValues().get(1);
        ByteArrayInputStream actualUprisingInputStream = new ByteArrayInputStream(actualUprisingMessage.getFile());
        PdfReader actualUprisingPdfReader = new PdfReader(actualUprisingInputStream);

        assertEquals(TestConstants.TOPIC_ID_UPRISING, actualUprisingMessage.getTopicId());
        assertEquals(TestConstants.CHAT_ID, actualUprisingMessage.getChatId());
        assertEquals("Рейтинг за 10\\.2010:", actualUprisingMessage.getText());
        assertArrayEquals(referenceFileBytes, actualUprisingPdfReader.getPageContent(1));

        FileMessageDto actualClassicMessage = messageCaptor.getAllValues().get(0);
        ByteArrayInputStream actualClassicInputStream = new ByteArrayInputStream(actualClassicMessage.getFile());
        PdfReader actualClassicPdfReader = new PdfReader(actualClassicInputStream);

        assertEquals(TestConstants.TOPIC_ID_CLASSIC, actualClassicMessage.getTopicId());
        assertEquals(TestConstants.CHAT_ID, actualClassicMessage.getChatId());
        assertEquals("Рейтинг за 10\\.2010:", actualClassicMessage.getText());
        assertTrue(actualClassicPdfReader.getPageContent(1).length > 0);

        actualClassicInputStream.close();
        actualClassicPdfReader.close();
        actualUprisingInputStream.close();
        actualUprisingPdfReader.close();
    }

    @Test
    void shouldRunServiceWhenTodayIsFirstDayOfTheMonth() {
        LocalDateTime firstDay = LocalDate.of(2010, 10, 1).atTime(15, 0);
        Clock fixedClock = Clock.fixed(firstDay.toInstant(ZoneOffset.of("+03:00")), ZoneOffset.of("+03:00"));
        doReturn(fixedClock.instant()).when(clock).instant();

        task.run();

        verify(ratingReportPdfService, times(2)).createRating(any(), any(), any(), any());
    }

    @Test
    void shouldNotRunServiceWhenTodayIsNotFirstDayOfTheMonth() {
        LocalDateTime firstDay = LocalDate.of(2010, 10, 3).atTime(15, 0);
        Clock fixedClock = Clock.fixed(firstDay.toInstant(ZoneOffset.of("+03:00")), ZoneOffset.of("+03:00"));
        doReturn(fixedClock.instant()).when(clock).instant();

        task.run();

        verify(ratingReportPdfService, never()).createRating(any(), any(), any(), any());
    }
}
