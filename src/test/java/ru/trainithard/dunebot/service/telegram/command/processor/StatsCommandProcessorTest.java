package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.PlayerRating;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.repository.PlayerRatingRepository;
import ru.trainithard.dunebot.repository.PlayerRepository;
import ru.trainithard.dunebot.service.messaging.ExternalMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.messaging.ExternalMessageFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class StatsCommandProcessorTest extends TestContextMock {
    private static final Instant NOW = LocalDate.of(2010, 10, 25)
            .atTime(15, 0, 0).toInstant(ZoneOffset.UTC);

    @MockBean
    private Clock clock;
    @SpyBean
    private PlayerRatingRepository playerRatingRepository;
    @SpyBean
    private PlayerRepository playerRepository;
    @MockBean
    private ExternalMessageFactory messageFactory;
    @Autowired
    private StatsCommandProcessor processor;
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10000, 10000, 12345, 'st_pl1', 'name1', 'l1', 'ef1', 'en1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10001, 10001, 12345, 'st_pl2', 'name2', 'l2', 'ef2', 'en2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10002, 10002, 12345, 'st_pl3', 'name3', 'l3', 'ef3', 'en3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10003, 10003, 12345, 'st_pl4', 'name4', 'l4', 'ef4', 'en4', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10004, 10004, 12345, 'st_pl5', 'name5', 'l5', 'ef5', 'en5', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10005, 10005, 12345, 'st_pl6', 'name6', 'l6', 'ef6', 'en6', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10006, 10006, 12345, 'st_pl7', 'name7', 'l7', 'ef7', 'en7', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10007, 10007, 12345, 'st_pl8', 'name8', 'l8', 'ef8', 'en8', '2010-10-10') ");

        jdbcTemplate.execute("insert into player_ratings (id, player_id, rating_date, matches_count, efficiency, " +
                             "win_rate, first_place_count, second_place_count, third_place_count, fourth_place_count, " +
                             "current_strike_length, max_strike_length, is_previously_won, created_at) values " +
                             "(10000, 10000, '2010-10-31', 4, 1.0, 1, 1, 2, 3, 4, 0, 1, false, '2009-12-01')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, rating_date, matches_count, efficiency, " +
                             "win_rate, first_place_count, second_place_count, third_place_count, fourth_place_count, " +
                             "current_strike_length, max_strike_length, is_previously_won, created_at) values " +
                             "(10001, 10001, '2010-10-15', 10, 1.3, 1, 1, 2, 3, 4, 0, 1, false, '2010-01-01')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, rating_date, matches_count, efficiency, " +
                             "win_rate, first_place_count, second_place_count, third_place_count, fourth_place_count, " +
                             "current_strike_length, max_strike_length, is_previously_won, created_at) values " +
                             "(10002, 10002, '2010-10-20', 7, 1.9, 1.2, 1, 2, 3, 4, 0, 1, false, '2010-01-01')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, rating_date, matches_count, efficiency, " +
                             "win_rate, first_place_count, second_place_count, third_place_count, fourth_place_count, " +
                             "current_strike_length, max_strike_length, is_previously_won, created_at) values " +
                             "(10003, 10003, '2010-10-10', 10, 1.0, 1.2, 1, 2, 3, 4, 0, 1, false, '2010-01-01')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, rating_date, matches_count, efficiency, " +
                             "win_rate, first_place_count, second_place_count, third_place_count, fourth_place_count, " +
                             "current_strike_length, max_strike_length, is_previously_won, created_at) values " +
                             "(10004, 10004, '2010-10-15', 10, 1.8, 1.9, 1, 2, 3, 4, 0, 1, false, '2010-01-01')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, rating_date, matches_count, efficiency, " +
                             "win_rate, first_place_count, second_place_count, third_place_count, fourth_place_count, " +
                             "current_strike_length, max_strike_length, is_previously_won, created_at) values " +
                             "(10005, 10005, '2010-10-20', 10, 1.4, 1, 1, 2, 3, 4, 0, 1, false, '2010-01-01')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, rating_date, matches_count, efficiency, " +
                             "win_rate, first_place_count, second_place_count, third_place_count, fourth_place_count, " +
                             "current_strike_length, max_strike_length, is_previously_won, created_at) values " +
                             "(10006, 10006, '2010-10-10', 10, 1.6, 1, 1, 2, 3, 4, 0, 1, false, '2010-01-01')");
        jdbcTemplate.execute("insert into player_ratings (id, player_id, rating_date, matches_count, efficiency, " +
                             "win_rate, first_place_count, second_place_count, third_place_count, fourth_place_count, " +
                             "current_strike_length, max_strike_length, is_previously_won, created_at) values " +
                             "(10007, 10007, '2010-10-01', 6, 2.0, 1, 1, 2, 3, 4, 0, 1, false, '2010-01-01')");

        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) values " +
                             "(10000, '" + AppSettingKey.RATING_STAT_ROWS_COUNT + "', 5, '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) values " +
                             "(10001, '" + AppSettingKey.MONTHLY_MATCHES_THRESHOLD + "', 10, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from player_ratings where id between 10000 and 10007");
        jdbcTemplate.execute("delete from players where id between 10000 and 10007");
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10001");
        cacheManager.getCache("playerRatings").clear();
    }

    @Test
    void shouldNotSelectOtherMonthRatings() {
        doReturn(new ExternalMessage("abc")).when(messageFactory).getNoRatingsMessage();
        jdbcTemplate.execute("update player_ratings set rating_date = '2010-11-01' where id between 10000 and 10007");

        processor.process(getCommandMessage(10005L));

        verify(playerRatingRepository)
                .findAllBy(LocalDate.of(2010, 10, 1), LocalDate.of(2010, 10, 31));
    }

    @Test
    void shouldDoNothingWhenNoRatingsForMonth() {
        Clock fixedClock = Clock.fixed(NOW.plus(90, ChronoUnit.DAYS), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        doReturn(new ExternalMessage("abc")).when(messageFactory).getNoRatingsMessage();

        processor.process(getCommandMessage(10005L));

        verifyNoInteractions(playerRepository);
    }

    @Test
    void shouldSendNoRatingsCalculatedMessageWhenNoRatingsForMonth() {
        Clock fixedClock = Clock.fixed(NOW.plus(90, ChronoUnit.DAYS), ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();
        doReturn(new ExternalMessage("abc")).when(messageFactory).getNoRatingsMessage();

        processor.process(getCommandMessage(10005L));

        verify(messagingService).sendMessageAsync(argThat(message -> message.getText().equals("abc")));
    }

    @Test
    void shouldSendNoOwnedRatingsMessageWhenNoRatingsForPlayer() {
        doReturn(new ExternalMessage("abc")).when(messageFactory).getNoOwnedRatingsMessage();
        jdbcTemplate.execute("delete from player_ratings where player_id = 10005");

        processor.process(getCommandMessage(10005));

        verify(messagingService).sendMessageAsync(argThat(message -> message.getText().equals("abc")));
    }

    @Test
    void shouldPassClosestPlayersWhenPlayerIsInMidstOfRating() {
        doReturn(new ExternalMessage("abc")).when(messageFactory).getRatingStatsMessage(any(), anyLong());

        processor.process(getCommandMessage(10001));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(messageFactory).getRatingStatsMessage(ratingsCaptor.capture(), anyLong());
        List<PlayerRating> actualRatings = ratingsCaptor.getValue();

        assertThat(actualRatings)
                .extracting(playerRating -> playerRating.getPlayer().getId())
                .containsExactly(10006L, 10005L, 10001L, 10003L, 10007L);
    }

    @Test
    void shouldPassClosestPlayersWhenPlayerIsInTheEnd() {
        doReturn(new ExternalMessage("abc")).when(messageFactory).getRatingStatsMessage(any(), anyLong());

        processor.process(getCommandMessage(10002));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(messageFactory).getRatingStatsMessage(ratingsCaptor.capture(), anyLong());
        List<PlayerRating> actualRatings = ratingsCaptor.getValue();

        assertThat(actualRatings)
                .extracting(playerRating -> playerRating.getPlayer().getId())
                .containsExactly(10001L, 10003L, 10007L, 10002L, 10000L);
    }

    @Test
    void shouldPassClosestPlayersWhenPlayerIsInTheBeginning() {
        doReturn(new ExternalMessage("abc")).when(messageFactory).getRatingStatsMessage(any(), anyLong());

        processor.process(getCommandMessage(10006));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(messageFactory).getRatingStatsMessage(ratingsCaptor.capture(), anyLong());
        List<PlayerRating> actualRatings = ratingsCaptor.getValue();

        assertThat(actualRatings)
                .extracting(playerRating -> playerRating.getPlayer().getId())
                .containsExactly(10004L, 10006L, 10005L, 10001L, 10003L);
    }

    @Test
    void shouldReturnAllPlayersWhenRatingsCountLesserThanSelectionSize() {
        doReturn(new ExternalMessage("abc")).when(messageFactory).getRatingStatsMessage(any(), anyLong());
        jdbcTemplate.execute("delete from player_ratings where id between 10000 and 10004");

        processor.process(getCommandMessage(10006));

        ArgumentCaptor<List<PlayerRating>> ratingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(messageFactory).getRatingStatsMessage(ratingsCaptor.capture(), anyLong());
        List<PlayerRating> actualRatings = ratingsCaptor.getValue();

        assertThat(actualRatings)
                .extracting(playerRating -> playerRating.getPlayer().getId())
                .containsExactly(10006L, 10005L, 10007L);
    }

    private CommandMessage getCommandMessage(long userId) {
        Message message = new Message();
        message.setMessageId(12345);
        message.setText("/stats");
        Chat chat = new Chat();
        chat.setId(112233L);
        chat.setType(ChatType.PRIVATE.getValue());
        message.setChat(chat);
        User user = new User();
        user.setId(userId);
        message.setFrom(user);
        return CommandMessage.getMessageInstance(message);
    }
}
