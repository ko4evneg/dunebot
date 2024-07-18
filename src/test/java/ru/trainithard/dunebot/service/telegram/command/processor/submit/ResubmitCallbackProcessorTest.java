package ru.trainithard.dunebot.service.telegram.command.processor.submit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.validator.SubmitMatchValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ResubmitCallbackProcessorTest extends TestContextMock {
    private static final long CHAT_ID = 12000L;
    private static final Long USER_ID_1 = 11000L;
    private static final long USER_ID_2 = 11001L;
    private final CommandMessage resubmitCallback = getCommandMessage(USER_ID_1);

    @Autowired
    private ResubmitCallbackProcessor processor;
    @MockBean
    private SubmitMatchValidator validator;
    @SpyBean
    private MatchRepository matchRepository;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + USER_ID_1 + ", " + USER_ID_1 + ", 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, " + USER_ID_2 + ", 10001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 10002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 10003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, " + CHAT_ID + ", '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 10000, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10002, 'ExternalMessageId', 12345, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, external_submit_id, owner_id, mod_type, state, positive_answers_count, submits_retry_count, submitter_id, created_at) " +
                             "values (15000, 10000, 10001, 10002, 10000, '" + ModType.CLASSIC + "', '" + MatchState.SUBMITTED + "', 4, 1, 10003,'2010-10-10') ");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10200, 'la leader 1', 'la leader 1', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10201, 'la leader 2', 'la leader 2', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10202, 'la leader 3', 'la leader 3', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, short_name, mod_type, created_at) values " +
                             "(10203, 'la leader 4', 'la leader 4', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, leader, created_at) " +
                             "values (10000, 15000, 10000, 1, 10200, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, leader, created_at) " +
                             "values (10001, 15000, 10001, 2, 10201, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, leader, created_at) " +
                             "values (10002, 15000, 10002, 3, 10202, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, place, leader, created_at) " +
                             "values (10003, 15000, 10003, 4, 10203, '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10000, '" + AppSettingKey.SUBMIT_TIMEOUT + "', 120, '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10001, '" + AppSettingKey.SUBMIT_TIMEOUT_WARNING_NOTIFICATION + "', 9, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10001");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from leaders where id between 10200 and 10203");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006");
    }

    @Test
    void shouldValidateMatch() {
        CommandMessage commandMessage = resubmitCallback;

        processor.process(commandMessage);

        InOrder inOrder = inOrder(validator, matchRepository);
        inOrder.verify(validator).validateReSubmitMatch(eq(commandMessage), any());
        inOrder.verify(matchRepository).save(any());
    }

    @Test
    void shouldIncreaseMatchResubmitCounterOnResubmit() {
        processor.process(resubmitCallback);

        Integer actualResubmits = jdbcTemplate.queryForObject("select submits_retry_count from matches where id = 15000", Integer.class);

        assertThat(actualResubmits).isEqualTo(2);
    }

    @Test
    void shouldReSetSubmitterOnResubmit() {
        processor.process(resubmitCallback);

        Long actualSubmitter = jdbcTemplate.queryForObject("select submitter_id from matches where id = 15000", Long.class);

        assertThat(actualSubmitter).isEqualTo(10000L);
    }

    @Test
    void shouldSetOnSubmitStateOnResubmit() {
        processor.process(resubmitCallback);

        MatchState actualState = jdbcTemplate.queryForObject("select state from matches where id = 15000", MatchState.class);

        assertThat(actualState).isEqualTo(MatchState.ON_SUBMIT);
    }

    @Test
    void shouldResetMatchPlayersDataOnResubmit() {
        processor.process(resubmitCallback);

        Integer actualResettedPlayersCount = jdbcTemplate.queryForObject("select count(*) from match_players where match_id = 15000 " +
                                                                         "and leader is null and place is null", Integer.class);

        assertThat(actualResettedPlayersCount).isEqualTo(4);
    }

    @Test
    void shouldSendMessageToSubmitInitiator() {
        processor.process(resubmitCallback);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();

        assertThat(actualMessageDto.getChatId()).isEqualTo(USER_ID_1.toString());
    }

    @Test
    void shouldSetMatchSubmitterOnSubmit() {
        processor.process(resubmitCallback);

        Long actualSubmittedId = jdbcTemplate.queryForObject("select submitter_id from matches where id = 15000", Long.class);

        assertThat(actualSubmittedId).isEqualTo(10000);
    }

    @Test
    void shouldDeleteOldSubmitMessageOnResubmit() {
        processor.process(resubmitCallback);

        verify(messagingService).deleteMessageAsync(argThat(messageId -> messageId.getMessageId() == 12345));
    }

    @Test
    void shouldSendCorrectSubmitMessage() {
        processor.process(resubmitCallback);

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getValue();
        List<List<ButtonDto>> linedButtons = actualMessageDto.getKeyboard();

        assertThat(actualMessageDto.getText()).isEqualTo("""
                Регистрация результатов для *матча 15000*\\. \
                Нажмите по очереди кнопки с именами участвовавших игроков, \
                начиная от победителя и заканчивая последним местом\\.""");
        assertThat(linedButtons)
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactlyInAnyOrder(
                        tuple("name1 (st_pl1) l1", "15000_SP_10000"),
                        tuple("name2 (st_pl2) l2", "15000_SP_10001"),
                        tuple("name3 (st_pl3) l3", "15000_SP_10002"),
                        tuple("name4 (st_pl4) l4", "15000_SP_10003")
                );
    }

    @Test
    void shouldReturnCorrectCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.RESUBMIT_CALLBACK);
    }

    private CommandMessage getCommandMessage(long userId) {
        User user = new User();
        user.setId(userId);
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setFrom(user);
        callbackQuery.setData("15000_RSC_" + userId);
        return CommandMessage.getCallbackInstance(callbackQuery);
    }
}
