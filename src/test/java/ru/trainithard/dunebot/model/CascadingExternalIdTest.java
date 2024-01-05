package ru.trainithard.dunebot.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.trainithard.dunebot.model.messaging.ExternalPollId;
import ru.trainithard.dunebot.repository.MatchPlayerRepository;
import ru.trainithard.dunebot.repository.MatchRepository;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Transactional(propagation = Propagation.SUPPORTS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CascadingExternalIdTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private MatchPlayerRepository matchPlayerRepository;

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where player_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000 or external_poll_id in (select id from external_messages where message_id between 10000 and 10010)");
        jdbcTemplate.execute("delete from external_messages where message_id between 10000 and 10010");
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @Test
    void shouldSaveExternalIdWithMatch() {
        Match match = new Match();
        match.setExternalPollId(new ExternalPollId(10000, 10001L, "10002", 10003));
        match.setExternalStartId(new ExternalPollId(10004, 10005L, "10006", 10007));
        match.setModType(ModType.CLASSIC);
        matchRepository.save(match);

        Boolean isPollIdExist = jdbcTemplate.queryForObject("select exists(select 1 from external_messages where " +
                "message_id = 10000 and chat_id = 10001 and poll_id = '10002' and reply_id = 10003)", Boolean.class);
        Boolean isSubmitIdExist = jdbcTemplate.queryForObject("select exists(select 1 from external_messages where " +
                "message_id = 10004 and chat_id = 10005 and poll_id = '10006' and reply_id = 10007)", Boolean.class);

        assertNotNull(isPollIdExist);
        assertNotNull(isSubmitIdExist);
        assertTrue(isPollIdExist);
        assertTrue(isSubmitIdExist);
    }

    @Test
    void shouldDeleteExternalIdWithMatch() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 10001, 10002 , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, 10001, 10002, 10003, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10001, 'ExternalMessageId', 10001, 10004, 10005, 10006, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, created_at) " +
                "values (10000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '2010-10-10') ");

        matchRepository.deleteById(10000L);

        Integer actualMessagesCount = jdbcTemplate.queryForObject("select count(*) from external_messages where message_id in (10000, 10001)", Integer.class);

        assertEquals(0, actualMessagesCount);
    }

    @Test
    void shouldDeleteDetachedExternalIdWithMatch() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 10001, 10002 , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, 10001, 10002, 10003, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10001, 'ExternalMessageId', 10001, 10004, 10005, 10006, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, created_at) " +
                "values (10000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '2010-10-10') ");

        Match match = matchRepository.findById(10000L).orElseThrow();
        match.setExternalStartId(null);
        match.setExternalPollId(null);
        matchRepository.save(match);
        matchRepository.deleteById(10000L);

        Integer actualMessagesCount = jdbcTemplate.queryForObject("select count(*) from external_messages where message_id in (10000, 10001)", Integer.class);

        assertEquals(0, actualMessagesCount);
    }

    @Test
    void shouldUpdateExternalIdWithMatch() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 10001, 10002 , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, 10001, 10002, 10003, '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10001, 'ExternalMessageId', 10001, 10004, 10005, 10006, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, created_at) " +
                "values (10000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '2010-10-10') ");

        Match match = matchRepository.findById(10000L).orElseThrow();
        match.getExternalPollId().setChatId(12345L);
        match.getExternalStartId().setChatId(12345L);
        matchRepository.save(match);

        Integer actualMessagesCount = jdbcTemplate.queryForObject("select count(*) from external_messages where chat_id = 12345", Integer.class);

        assertEquals(2, actualMessagesCount);
    }

    @Test
    void shouldSaveExternalIdWithMatchPlayer() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 10001, 10002 , 'st_pl1', 'name1', '2010-10-10') ");
        Match match = new Match();
        match.setExternalPollId(new ExternalPollId(10001, 10001L, "10001", 10001));
        match.setExternalStartId(new ExternalPollId(10002, 10002L, "10002", 10002));
        match.setModType(ModType.CLASSIC);
        matchRepository.save(match);
        Player player = new Player();
        player.setId(10000L);
        MatchPlayer matchPlayer = new MatchPlayer(match, player);
        matchPlayer.setSubmitMessageId(new ExternalPollId(10010, 10011L, "10012", 10013));
        matchPlayerRepository.save(matchPlayer);

        Boolean isSubmitIdExist = jdbcTemplate.queryForObject("select exists(select 1 from external_messages where " +
                "message_id = 10010 and chat_id = 10011 and poll_id = '10012' and reply_id = 10013)", Boolean.class);

        assertNotNull(isSubmitIdExist);
        assertTrue(isSubmitIdExist);
    }

    @Test
    void shouldDeleteExternalIdWithMatchPlayer() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 10001, 10002 , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, 10001, 10002, 10003, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, created_at) " +
                "values (10000,10000, '" + ModType.CLASSIC + "', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                "values (10000, 10000, 10000, 10000, '2010-10-10')");

        matchPlayerRepository.deleteById(10000L);

        Integer actualMessagesCount = jdbcTemplate.queryForObject("select count(*) from external_messages where message_id = 10000", Integer.class);

        assertEquals(0, actualMessagesCount);
    }

    @Test
    void shouldDeleteDetachedExternalIdWithMatchPlayer() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 10001, 10002 , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, 10001, 10002, 10003, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, created_at) " +
                "values (10000,10000, '" + ModType.CLASSIC + "', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                "values (10000, 10000, 10000, 10000, '2010-10-10')");

        MatchPlayer matchPlayer = matchPlayerRepository.findById(10000L).orElseThrow();
        matchPlayer.getSubmitMessageId().setChatId(12345L);
        matchPlayerRepository.save(matchPlayer);

        Integer actualMessagesCount = jdbcTemplate.queryForObject("select count(*) from external_messages where chat_id = 12345", Integer.class);

        assertEquals(1, actualMessagesCount);
    }

    @Test
    void shouldUpdateExternalIdWithMatchPlayer() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, 10001, 10002 , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                "values (10000, 'ExternalPollId', 10000, 10001, 10002, 10003, '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, created_at) " +
                "values (10000,10000, '" + ModType.CLASSIC + "', '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, external_submit_id, created_at) " +
                "values (10000, 10000, 10000, 10000, '2010-10-10')");

        MatchPlayer matchPlayer = matchPlayerRepository.findById(10000L).orElseThrow();
        matchPlayer.setSubmitMessageId(null);
        matchPlayerRepository.save(matchPlayer);
        matchPlayerRepository.deleteById(10000L);

        Integer actualMessagesCount = jdbcTemplate.queryForObject("select count(*) from external_messages where message_id = 10000", Integer.class);

        assertEquals(0, actualMessagesCount);
    }
}
