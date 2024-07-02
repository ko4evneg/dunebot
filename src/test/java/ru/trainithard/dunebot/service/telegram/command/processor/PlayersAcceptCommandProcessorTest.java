package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.AppSettingKey;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.dto.ButtonDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class PlayersAcceptCommandProcessorTest extends TestContextMock {
    private static final Long CHAT_ID = 12000L;
    private static final Long USER_ID = 11000L;

    @Autowired
    private PlayersAcceptCommandProcessor processor;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + USER_ID + ", " + USER_ID + ", 'st_pl1', 'name1', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 11001, 12001, 'st_pl2', 'name2', 'l2', 'e2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10002, 11002, 12002, 'st_pl3', 'name3', 'l3', 'e3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10003, 11003, 12003, 'st_pl4', 'name4', 'l4', 'e4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', 10000, " + CHAT_ID + ", '10000', '2020-10-10')");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 10000, " + CHAT_ID + ", '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, external_start_id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (15000, 10000, 10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 4, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10100, 15000, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10101, 15000, 10001, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10102, 15000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10103, 15000, 10003, '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10200, 'la leader 1', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into leaders (id, name, mod_type, created_at) values " +
                             "(10201, 'la leader 2', '" + ModType.CLASSIC + "', '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) " +
                             "values (10000, '" + AppSettingKey.FINISH_MATCH_TIMEOUT + "', 120, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id = 10000");
        jdbcTemplate.execute("delete from leaders where id between 10200 and 10201");
        jdbcTemplate.execute("delete from match_players where match_id in (15000, 15001)");
        jdbcTemplate.execute("delete from matches where id in (15000, 15001)");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004");
        jdbcTemplate.execute("delete from external_messages where chat_id between 12000 and 12006");
    }

    @ParameterizedTest
    @ValueSource(longs = {10100, 10101, 10102, 10103})
    void shouldSetFirstPlaceWhenNoPlacesSelectedYet(long matchPlayerId) {
        processor.process(getCallbackMessage("15000_SP_" + matchPlayerId));

        Integer actualPlace = jdbcTemplate.queryForObject("select place from match_players where id = " + matchPlayerId, Integer.class);

        assertThat(actualPlace).isEqualTo(1);
    }

    @Test
    void shouldSetThirdPlaceWhenTwoPlacesSelected() {
        jdbcTemplate.execute("update match_players set place = 1 where id = 10101");
        jdbcTemplate.execute("update match_players set place = 2 where id = 10102");

        processor.process(getCallbackMessage("15000_SP_" + 10100));

        Integer actualPlace = jdbcTemplate.queryForObject("select place from match_players where id = 10100", Integer.class);

        assertThat(actualPlace).isEqualTo(3);
    }

    @Test
    void shouldSendAcceptPlayerSubmitMessageOnLastPlaceSubmit() {
        jdbcTemplate.execute("update match_players set place = 1 where id = 10101");
        jdbcTemplate.execute("update match_players set place = 2 where id = 10102");
        jdbcTemplate.execute("update match_players set place = 3 where id = 10103");

        processor.process(getCallbackMessage("15000_SP_10100"));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(2)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getAllValues().get(0);

        assertThat(actualMessageDto.getChatId()).isEqualTo(USER_ID.toString());
        assertThat(actualMessageDto.getText()).isEqualTo("""
                Следующие результаты зарегистрированы для *матча 15000*:
                1: name2 \\(st\\_pl2\\) l2
                2: name3 \\(st\\_pl3\\) l3
                3: name4 \\(st\\_pl4\\) l4
                4: name1 \\(st\\_pl1\\) l1""");
    }

    @Test
    void shouldSendLeadersSubmitMessageOnLastPlaceSubmit() {
        jdbcTemplate.execute("update match_players set place = 1 where id = 10101");
        jdbcTemplate.execute("update match_players set place = 2 where id = 10102");
        jdbcTemplate.execute("update match_players set place = 3 where id = 10103");

        processor.process(getCallbackMessage("15000_SP_10100"));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(2)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto actualMessageDto = messageDtoCaptor.getAllValues().get(1);

        assertThat(actualMessageDto.getChatId()).isEqualTo(USER_ID.toString());
        assertThat(actualMessageDto.getText()).isEqualTo("""
                Теперь выберите лидеров для *матча 15000*\\. Нажмите по очереди кнопки с именами лидеров, \
                начиная от лидера победителя и заканчивая лидером, занявшим последнее место\\.""");
        assertThat(actualMessageDto.getKeyboard())
                .flatExtracting(buttonDtos -> buttonDtos)
                .extracting(ButtonDto::getText, ButtonDto::getCallback)
                .containsExactly(
                        tuple("la leader 1", "15000_SL_10200"),
                        tuple("la leader 2", "15000_SL_10201")
                );
    }

    @Test
    void shouldThrowWhenPlayerIsSubmittedTwice() {
        jdbcTemplate.execute("update match_players set place = 1 where id = 10101");
        CommandMessage callbackMessage = getCallbackMessage("15000_SP_10101");

        assertThatThrownBy(() -> processor.process(callbackMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Вы уже назначили игроку name2 (st_pl2) l2 место 1. " +
                            "Выберите другого игрока, или используйте команду '/resubmit 15000', чтобы начать заново.");
    }

    @ParameterizedTest
    @EnumSource(value = MatchState.class, mode = EnumSource.Mode.EXCLUDE, names = {"NEW", "ON_SUBMIT"})
    void shouldThrowWhenMatchInWrongState(MatchState matchState) {
        jdbcTemplate.execute("update matches set state = '" + matchState + "' where id = 15000");
        CommandMessage callbackMessage = getCallbackMessage("15000_SP_" + 10101);

        assertThatThrownBy(() -> processor.process(callbackMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Матч 15000 уже завершен. Регистрация результата более невозможна.");
    }

    @Test
    void shouldNotChangePlaceWhenPlayerIsSubmittedTwice() {
        jdbcTemplate.execute("update match_players set place = 1 where id = 10101");

        try {
            processor.process(getCallbackMessage("15000_SP_10101"));
        } catch (Exception ignored) {
        }

        Integer actualPlace = jdbcTemplate.queryForObject("select place from match_players where id = 10101", Integer.class);
        assertThat(actualPlace).isEqualTo(1);
    }

    @Test
    void shouldReturnPlayerAcceptCommand() {
        Command actualCommand = processor.getCommand();
        assertThat(actualCommand).isEqualTo(Command.PLAYER_ACCEPT);
    }

    private static CommandMessage getCallbackMessage(String callbackData) {
        User user = new User();
        user.setId(USER_ID);
        Message message = new Message();
        message.setFrom(user);
        Chat chat = new Chat();
        chat.setId(USER_ID);
        message.setChat(chat);
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData(callbackData);
        callbackQuery.setFrom(user);
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return CommandMessage.getCallbackInstance(callbackQuery);
    }
}
