package ru.trainithard.dunebot.service.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.DubeBotException;
import ru.trainithard.dunebot.model.Command;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@SpringBootTest
class TelegramUpdateProcessorTest extends TestContextMock {
    @Autowired
    private TelegramUpdateProcessor updateProcessor;
    @MockBean
    private MessagingService messagingService;
    @MockBean
    private TelegramMessageCommandValidator validator;

    private static final String COMMAND_NEW_UP4 = "/" + Command.NEW + " up4";
    private static final long TELEGRAM_USER_ID_1 = 10000L;
    private static final long TELEGRAM_USER_ID_2 = 10001L;
    private static final long TELEGRAM_CHAT_ID_1 = 9000L;
    private static final long TELEGRAM_CHAT_ID_2 = 9001L;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + TELEGRAM_USER_ID_1 + ", " + TELEGRAM_CHAT_ID_1 + " , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, " + TELEGRAM_USER_ID_2 + ", " + TELEGRAM_CHAT_ID_2 + " , 'st_pl2', 'name2', '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id in(10000, 10001)");
    }

    @Test
    void shouldSendTelegramMessageOnWrongCommand() {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, "/wrongcommand")).thenReturn(null);
        doCallRealMethod().when(validator).validate(any());

        updateProcessor.process();

        verify(messagingService, times(1)).sendMessageAsync(argThat(messageDto ->
                "9000".equals(messageDto.getChatId()) && "Неверная команда!".equals(messageDto.getText())));
    }

    @Test
    void shouldIncludeReplyMessageIdOnTopicWrongCommandReceive() {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, 1234, "/wrongcommand")).thenReturn(null);
        doCallRealMethod().when(validator).validate(any());

        updateProcessor.process();

        verify(messagingService, times(1)).sendMessageAsync(argThat(messageDto ->
                messageDto.getReplyMessageId().equals(1234)));
    }

    @ParameterizedTest
    @MethodSource("exceptionsProvider")
    void shouldNotThrowOnException(Class<? extends Exception> aClass) {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, "/up")).thenReturn(null);
        doThrow(aClass).when(validator).validate(any(CommandMessage.class));

        assertDoesNotThrow(() -> updateProcessor.process());
    }

    @ParameterizedTest
    @MethodSource("exceptionsProvider")
    void shouldMoveToNextUpdateOnException(Class<? extends Exception> aClass) {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, "/up")).thenReturn(getUpdate(TELEGRAM_USER_ID_2, TELEGRAM_CHAT_ID_2, null, COMMAND_NEW_UP4)).thenReturn(null);
        doThrow(aClass).when(validator).validate(any(CommandMessage.class));

        updateProcessor.process();

        verify(validator, times(1)).validate(argThat(commandMessage ->
                TELEGRAM_USER_ID_2 == commandMessage.getUserId() &&
                        TELEGRAM_CHAT_ID_2 == commandMessage.getChatId() &&
                        commandMessage.getCommand() == Command.NEW &&
                        "up4".equals(commandMessage.getArgument(1))));
    }

    private static Stream<Arguments> exceptionsProvider() {
        return Stream.of(
                Arguments.of(RuntimeException.class),
                Arguments.of(DubeBotException.class),
                Arguments.of(NullPointerException.class)
        );
    }

    @Test
    void shouldMoveToNextUpdateWithoutException() {
        when(telegramBot.poll()).thenReturn(getUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, COMMAND_NEW_UP4)).thenReturn(getUpdate(TELEGRAM_USER_ID_2, TELEGRAM_CHAT_ID_2, null, COMMAND_NEW_UP4)).thenReturn(null);

        updateProcessor.process();

        verify(validator, times(1)).validate(argThat(commandMessage ->
                TELEGRAM_USER_ID_1 == commandMessage.getUserId() && TELEGRAM_CHAT_ID_1 == commandMessage.getChatId()));
        verify(validator, times(1)).validate(argThat(commandMessage ->
                TELEGRAM_USER_ID_2 == commandMessage.getUserId() && TELEGRAM_CHAT_ID_2 == commandMessage.getChatId()));
    }

    private Update getUpdate(long telegramUserId, long telegramChatId, Integer replyId, String text) {
        User user = new User();
        user.setId(telegramUserId);
        Chat chat = new Chat();
        chat.setId(telegramChatId);
        chat.setType(ChatType.PRIVATE.getValue());
        Message reply = new Message();
        reply.setMessageId(replyId);
        Message message = new Message();
        message.setMessageId(10000);
        message.setChat(chat);
        message.setFrom(user);
        message.setText(text);
        message.setReplyToMessage(reply);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}
