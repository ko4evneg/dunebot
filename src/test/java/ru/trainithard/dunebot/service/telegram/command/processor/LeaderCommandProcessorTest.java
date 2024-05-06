package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileSystemUtils;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.factory.CommandMessageFactoryImpl;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
class LeaderCommandProcessorTest extends TestContextMock {
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final CommandMessageFactoryImpl factory = new CommandMessageFactoryImpl(matchRepository);

    @Autowired
    private LeaderCommandProcessor processor;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, 12345, 9000, 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
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
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10000, 'la leader 1', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10001, 'la leader 2', '" + ModType.CLASSIC + "', '2010-10-10')");
    }

    @AfterEach
    void afterEach() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("photos/11_10"));
        jdbcTemplate.execute("delete from match_players where match_id between 10000 and 10001");
        jdbcTemplate.execute("delete from matches where id between 10000 and 10001");
        jdbcTemplate.execute("delete from players where id between 10000 and 10003");
        jdbcTemplate.execute("delete from leaders where id between 10000 and 10001");
    }

    @ParameterizedTest
    @ValueSource(ints = {10000, 10001})
    void shouldSaveLeaderInMatchPlayer(long leaderId) {
        Update callbackQueryUpdate = getCallbackQueryUpdate(leaderId);
        CommandMessage commandMessage = factory.getInstance(callbackQueryUpdate);

        processor.process(commandMessage);

        Long actualMatchLeaderId = jdbcTemplate.queryForObject("select leader from match_players where id = 10000", Long.class);

        assertThat(leaderId).isEqualTo(actualMatchLeaderId);
    }

    @Test
    void shouldSendMessageOnLeaderRegistration() {
        Update callbackQueryUpdate = getCallbackQueryUpdate(10000L);
        CommandMessage commandMessage = factory.getInstance(callbackQueryUpdate);

        processor.process(commandMessage);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessageDto = messageCaptor.getValue();

        assertThat(actualMessageDto.getChatId()).isEqualTo("9000");
        assertThat(actualMessageDto.getText()).isEqualTo("Лидер зарегистрирован\\. Ожидайте регистрации мест других игроков\\.");
    }

    private static Update getCallbackQueryUpdate(long leaderId) {
        User user = new User();
        user.setId(12345L);
        Message message = new Message();
        message.setMessageId(9000);
        message.setFrom(user);
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData("10000_L_" + leaderId);
        callbackQuery.setFrom(user);
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return update;
    }

}
