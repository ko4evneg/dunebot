package ru.trainithard.dunebot.service.task;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.trainithard.dunebot.TestConstants;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.scheduler.DuneBotTaskId;
import ru.trainithard.dunebot.configuration.scheduler.DuneTaskType;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.service.messaging.dto.ExternalMessageDto;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class StartMatchTaskTest extends TestContextMock {
    private static final int REPLY_ID = 111001;
    private static final String POLL_ID = "100500";
    private static final long CHAT_ID = 100501L;
    private static final int TOPIC_ID = 100500;
    private static final long USER_1_ID = 12345L;
    private static final long USER_2_ID = 12346L;
    private static final long GUEST_ID = 12400L;
    //TODO: check need
    private static final Instant NOW = LocalDate.of(2010, 10, 10).atTime(15, 0, 0)
            .toInstant(ZoneOffset.UTC);

    @MockBean
    private Clock clock;
    @Autowired
    private DuneScheduledTaskFactory taskFactory;

    @BeforeEach
    @SneakyThrows
    void beforeEach() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        doReturn(fixedClock.instant()).when(clock).instant();
        doReturn(fixedClock.getZone()).when(clock).getZone();

        CompletableFuture<ExternalMessageDto> mockResponse = new CompletableFuture<>();
        mockResponse.complete(new ExternalMessageDto());
        doReturn(mockResponse).when(messagingService).sendMessageAsync(any());

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10000, " + USER_1_ID + ", 12345, 'st_pl1', 'name1', 'l1', 'ef1', 'en1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, created_at) " +
                             "values (10001, " + USER_2_ID + ", 12346, 'st_pl2', 'name2', 'l2', 'ef2', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10002, 12347, 12347, 'st_pl3', 'name3', 'l3', 'ef3', 'en3', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, " +
                             "last_name, external_first_name, external_name, created_at) " +
                             "values (10003, 12348, 12348, 'st_pl4', 'name4', 'l4', 'ef4', 'en4', '2010-10-10') ");
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, poll_id, created_at) " +
                             "values (10000, 'ExternalPollId', " + REPLY_ID + ", " + TestConstants.CHAT_ID + ", " + TOPIC_ID + ", '" + POLL_ID + "', '2020-10-10')");
        jdbcTemplate.execute("insert into matches (id, external_poll_id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (10000, 10000, 10000, '" + ModType.CLASSIC + "','" + MatchState.NEW + "', 1, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10000, 10000, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 10000, 10001, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10002, 10000, 10002, '2010-10-10')");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10003, 10000, 10003, '2010-10-10')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10001");
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000");
        jdbcTemplate.execute("delete from players where id between 10000 and 10004 or external_id in (" + USER_2_ID + ", " + GUEST_ID + ")");
        jdbcTemplate.execute("delete from external_messages where id between 10000 and 10001 or chat_id between 12345 and 12348 " +
                             "or chat_id in (" + CHAT_ID + ", " + GUEST_ID + ")");
    }

    @Test
    void shouldDoNothingWhenNoMatchFound() {
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10001");
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000");

        DunebotRunnable task = taskFactory.createInstance(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));
        task.run();
        
        verifyNoInteractions(messagingService);
    }

    @Test
    void shouldSendStartMessageToMatchTopic() {
        DunebotRunnable task = taskFactory.createInstance(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));
        task.run();

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getReplyMessageId, MessageDto::getKeyboard)
                .containsExactly(TestConstants.CHAT_ID, TOPIC_ID, REPLY_ID, null);
        assertThat(messageDto.getText()).isEqualTo("""
                *Матч 10000* собран\\. Участники:
                [@en1](tg://user?id=12345), [@ef2](tg://user?id=12346), [@en3](tg://user?id=12347), [@en4](tg://user?id=12348)""");
    }

    @Test
    void shouldSendStartMessageWithWarningToMatchTopicWhenGuestPlayersPresented() {
        jdbcTemplate.execute("update players set is_guest = true where id in (10000, 10001)");

        DunebotRunnable task = taskFactory.createInstance(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));
        task.run();

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getReplyMessageId, MessageDto::getKeyboard)
                .containsExactly(TestConstants.CHAT_ID, TOPIC_ID, REPLY_ID, null);
        assertThat(messageDto.getText()).isEqualTo("""
                *Матч 10000* собран\\. Участники:
                [@en3](tg://user?id=12347), [@en4](tg://user?id=12348)
                          
                *Внимание:* в матче есть незарегистрированные игроки\\. Они автоматически зарегистрированы под именем\
                 Vasya Pupkin и смогут подтвердить результаты матчей для регистрации результатов:
                [@en1](tg://user?id=12345), [@ef2](tg://user?id=12346)""");
    }

    @Test
    void shouldSendStartMessageWithWarningToMatchTopicWhenChatBlockedPlayersPresented() {
        jdbcTemplate.execute("update players set is_chat_blocked = true where id in (10001, 10002)");

        DunebotRunnable task = taskFactory.createInstance(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));
        task.run();

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getReplyMessageId, MessageDto::getKeyboard)
                .containsExactly(TestConstants.CHAT_ID, TOPIC_ID, REPLY_ID, null);
        assertThat(messageDto.getText()).isEqualTo("""
                *Матч 10000* собран\\. Участники:
                [@en1](tg://user?id=12345), [@en4](tg://user?id=12348)
                          
                *Особое внимание:* у этих игроков заблокированы чаты\\. Без их регистрации и добавлении в контакты бота*\
                 до начала регистрации результатов, завершить данный матч будет невозможно\\!*
                [@ef2](tg://user?id=12346), [@en3](tg://user?id=12347)""");
    }

    @Test
    void shouldSendStartMessageWithSpecialWarningToMatchTopicWhenGuestAndChatBlockedPlayersPresented() {
        jdbcTemplate.execute("update players set is_guest = true where id = 10000");
        jdbcTemplate.execute("update players set is_chat_blocked = true where id in (10001, 10002)");

        DunebotRunnable task = taskFactory.createInstance(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));
        task.run();

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getReplyMessageId, MessageDto::getKeyboard)
                .containsExactly(TestConstants.CHAT_ID, TOPIC_ID, REPLY_ID, null);
        assertThat(messageDto.getText()).isEqualTo("""
                *Матч 10000* собран\\. Участники:
                [@en4](tg://user?id=12348)
                                
                *Внимание:* в матче есть незарегистрированные игроки\\. Они автоматически зарегистрированы под именем\
                 Vasya Pupkin и смогут подтвердить результаты матчей для регистрации результатов:
                [@en1](tg://user?id=12345)
                                
                *Особое внимание:* у этих игроков заблокированы чаты\\. Без их регистрации и добавлении в контакты бота*\
                 до начала регистрации результатов, завершить данный матч будет невозможно\\!*
                [@ef2](tg://user?id=12346), [@en3](tg://user?id=12347)""");
    }

    @Test
    void shouldSendDeleteMessageWhenMatchAlreadyHasId() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + TOPIC_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 5, external_start_id = 10001 where id = 10000");

        DunebotRunnable task = taskFactory.createInstance(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));
        task.run();

        verify(messagingService).deleteMessageAsync(argThat(messageDto ->
                messageDto.getMessageId().equals(9000) && messageDto.getChatId().equals(CHAT_ID) && messageDto.getReplyId().equals(TOPIC_ID)));
    }

    @Test
    void shouldSaveNewStartMessageWhenMatchAlreadyHasStartMessage() {
        jdbcTemplate.execute("insert into external_messages (id, dtype, message_id, chat_id, reply_id, created_at) " +
                             "values (10001, 'ExternalMessageId', 9000, " + CHAT_ID + ", " + TOPIC_ID + ", '2020-10-10')");
        jdbcTemplate.execute("update matches set positive_answers_count = 5, external_start_id = 10001 where id = 10000");
        doReturn(CompletableFuture.completedFuture(getSubmitExternalMessage())).when(messagingService).sendMessageAsync(any(MessageDto.class));

        DunebotRunnable task = taskFactory.createInstance(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));
        task.run();

        ExternalMessageDto actualMessage = jdbcTemplate
                .queryForObject("select * from external_messages where chat_id = 12348", new BeanPropertyRowMapper<>(ExternalMessageDto.class));

        assertThat(actualMessage).isNotNull()
                .extracting(ExternalMessageDto::getChatId, ExternalMessageDto::getReplyId, ExternalMessageDto::getMessageId)
                .containsExactly(12348L, 11111, 22222);
    }

    @Test
    void shouldSaveStartMessageWhenMatchHasNoStartMessage() {
        jdbcTemplate.execute("update matches set positive_answers_count = 5, external_start_id = null where id = 10000");
        doReturn(CompletableFuture.completedFuture(getSubmitExternalMessage())).when(messagingService).sendMessageAsync(any(MessageDto.class));

        DunebotRunnable task = taskFactory.createInstance(new DuneBotTaskId(DuneTaskType.START_MESSAGE, 10000L));
        task.run();

        ExternalMessageDto actualMessage = jdbcTemplate
                .queryForObject("select * from external_messages where chat_id = 12348", new BeanPropertyRowMapper<>(ExternalMessageDto.class));

        assertThat(actualMessage).isNotNull()
                .extracting(ExternalMessageDto::getChatId, ExternalMessageDto::getReplyId, ExternalMessageDto::getMessageId)
                .containsExactly(12348L, 11111, 22222);
    }

    private ExternalMessageDto getSubmitExternalMessage() {
        Message replyMessage = new Message();
        replyMessage.setMessageId(11111);
        Chat chat = new Chat();
        chat.setId(12348L);
        Message message = new Message();
        message.setMessageId(22222);
        message.setReplyToMessage(replyMessage);
        message.setChat(chat);
        message.setMessageThreadId(TOPIC_ID);
        return new ExternalMessageDto(message);
    }
}
