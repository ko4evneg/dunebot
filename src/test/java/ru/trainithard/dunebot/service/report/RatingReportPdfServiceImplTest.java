package ru.trainithard.dunebot.service.report;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RatingReportPdfServiceImplTest extends TestContextMock {
    private static final String REPORT_NAME = "РЕЙТИНГ 10.2010";
    private static final LocalDate to = YearMonth.of(2010, 10).atEndOfMonth();
    private static final LocalDate from = YearMonth.of(2010, 10).atDay(1);

    @Autowired
    private RatingReportPdfService service;

    @BeforeEach
    void beforeEach() {
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
                             "values (10000, 'ExternalPollId', 10000, 12345, '10000', 9000, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10001, 'ExternalPollId', 10001, 12345, '10001', 9001, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10002, 'ExternalPollId', 10002, 12345, '10002', 9001, '2020-10-10')");

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
                             "values (10000, '" + AppSettingKey.MONTHLY_MATCHES_THRESHOLD + "', 15, '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10001, '" + AppSettingKey.CHAT_ID + "', '" + TestConstants.CHAT_ID + "', '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10002, '" + AppSettingKey.TOPIC_ID_CLASSIC + "', '" + 9000 + "', '2010-10-10')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) " +
                             "values (10003, '" + AppSettingKey.TOPIC_ID_UPRISING + "', '" + TestConstants.TOPIC_ID_UPRISING + "', '2010-10-10')");
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
    void shouldCreateCorrectReport() throws DocumentException, IOException {
        RatingReportPdf actualRatingPdf = service.createRating(from, to, ModType.UPRISING_4, REPORT_NAME);
        byte[] actualPdfContent = actualRatingPdf.getPdfBytes();

        PdfReader pdfReader = new PdfReader(actualPdfContent);
        PdfReader referencePdfReader = new PdfReader("src/test/resources/pdf/monthly_rate_example_2.pdf");
        byte[] actualBytes = pdfReader.getPageContent(1);
        byte[] expectedBytes = referencePdfReader.getPageContent(1);

        pdfReader.close();
        referencePdfReader.close();

        assertThat(actualBytes).isEqualTo(expectedBytes);
    }

    @Test
    void shouldNotConsiderAnotherModTypesForReport() throws DocumentException, IOException {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, reply_id, created_at) " +
                             "values (10003, 'ExternalPollId', 10003, " + "12345" + ", '10003', " + 9000 + ", '2020-10-10')");
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

        RatingReportPdf actualRatingPdf = service.createRating(from, to, ModType.UPRISING_4, REPORT_NAME);
        byte[] actualPdfContent = actualRatingPdf.getPdfBytes();

        PdfReader pdfReader = new PdfReader(actualPdfContent);
        PdfReader referencePdfReader = new PdfReader("src/test/resources/pdf/monthly_rate_example_2.pdf");
        byte[] actualBytes = pdfReader.getPageContent(1);
        byte[] expectedBytes = referencePdfReader.getPageContent(1);

        pdfReader.close();
        referencePdfReader.close();

        assertThat(actualBytes).isEqualTo(expectedBytes);
    }
}
