package ru.trainithard.dunebot.service.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.DuneBotException;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.ExternalPollDto;
import ru.trainithard.dunebot.service.messaging.dto.PollMessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;
import ru.trainithard.dunebot.service.telegram.command.CommandType;
import ru.trainithard.dunebot.service.telegram.factory.CommandMessageFactoryImpl;
import ru.trainithard.dunebot.service.telegram.factory.CommandProcessorFactory;
import ru.trainithard.dunebot.service.telegram.factory.ValidationStrategyFactory;
import ru.trainithard.dunebot.service.telegram.validator.CommonCommandMessageValidator;
import ru.trainithard.dunebot.service.telegram.validator.DefaultCommandMessageValidator;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@SpringBootTest
class TelegramUpdateProcessorTest extends TestContextMock {
    private static final String COMMAND_REFRESH_PROFILE = "/refresh_profile";
    private static final int TELEGRAM_REPLY_ID = 10010;
    private static final long TELEGRAM_USER_ID_1 = 10000L;
    private static final long TELEGRAM_USER_ID_2 = 10001L;
    private static final long TELEGRAM_CHAT_ID_1 = 9000L;
    private static final long TELEGRAM_CHAT_ID_2 = 9001L;

    @Autowired
    private TelegramUpdateProcessor updateProcessor;
    @Autowired
    private DefaultCommandMessageValidator defaultValidator;
    @MockBean
    private ValidationStrategyFactory validationStrategyFactory;
    @MockBean
    private CommandProcessorFactory commandProcessorFactory;
    @MockBean
    private CommandMessageFactoryImpl commandMessageFactory;
    @SpyBean
    private CommonCommandMessageValidator commonCommandMessageValidator;

    @BeforeEach
    void beforeEach() {
        doCallRealMethod().when(commandMessageFactory).getInstance(any());
        doCallRealMethod().when(commonCommandMessageValidator).validate(any());

        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + TELEGRAM_USER_ID_1 + ", " + TELEGRAM_CHAT_ID_1 + " , 'st_pl1', 'name1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10001, " + TELEGRAM_USER_ID_2 + ", " + TELEGRAM_CHAT_ID_2 + " , 'st_pl2', 'name2', '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id = 10000");
        jdbcTemplate.execute("delete from matches where id = 10000 or external_poll_id = (select id from external_messages where poll_id = '100001')");
        jdbcTemplate.execute("delete from players where id in(10000, 10001)");
        jdbcTemplate.execute("delete from external_messages where id in (10000)");
    }

    @Test
    void shouldSendTelegramMessageOnUnknownCommandException() {
        when(telegramBot.poll()).thenReturn(getTextUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, "/wrongcommand")).thenReturn(null);

        updateProcessor.process();

        verify(messagingService, times(1)).sendMessageAsync(argThat(messageDto ->
                Long.toString(TELEGRAM_CHAT_ID_1).equals(messageDto.getChatId()) && "Неверная команда!".equals(messageDto.getText())));
    }

    @Test
    void shouldIncludeReplyMessageIdOnTopicWrongCommandReceive() {
        when(telegramBot.poll()).thenReturn(getTextUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, 12345, "/wrongcommand")).thenReturn(null);

        updateProcessor.process();

        verify(messagingService, times(1)).sendMessageAsync(argThat(messageDto ->
                messageDto.getReplyMessageId() == 12345));
    }

    @Test
    void shouldDoNothingOnNotAcceptableUpdate() {
        when(telegramBot.poll()).thenReturn(getTextUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, "just text")).thenReturn(null);

        updateProcessor.process();

        verify(validationStrategyFactory, never()).getValidator(any());
        verify(commandProcessorFactory, never()).getProcessor(any());
    }

    @ParameterizedTest
    @MethodSource("exceptionsProvider")
    void shouldNotThrowOnException(Class<? extends Exception> anException) {
        when(telegramBot.poll()).thenReturn(getTextUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, "/up")).thenReturn(null);
        doThrow(anException).when(commandMessageFactory).getInstance(any());

        assertDoesNotThrow(() -> updateProcessor.process());
    }

    private static Stream<Arguments> exceptionsProvider() {
        return Stream.of(
                Arguments.of(RuntimeException.class),
                Arguments.of(DuneBotException.class),
                Arguments.of(NullPointerException.class)
        );
    }

    @Test
    void shouldMoveToNextUpdateWhenExceptionOccurs() {
        when(telegramBot.poll())
                .thenReturn(getTextUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, COMMAND_REFRESH_PROFILE))
                .thenReturn(getTextUpdate(TELEGRAM_USER_ID_2, TELEGRAM_CHAT_ID_2, null, COMMAND_REFRESH_PROFILE)).thenReturn(null);
        doThrow(new RuntimeException()).when(commandMessageFactory).getInstance(any());

        updateProcessor.process();

        verify(commandMessageFactory, times(1)).getInstance(argThat(update ->
                TELEGRAM_USER_ID_1 == update.getMessage().getFrom().getId() && TELEGRAM_CHAT_ID_1 == update.getMessage().getChatId()));
        verify(commandMessageFactory, times(1)).getInstance(argThat(update ->
                TELEGRAM_USER_ID_2 == update.getMessage().getFrom().getId() && TELEGRAM_CHAT_ID_2 == update.getMessage().getChatId()));
    }

    @Test
    void shouldDoSomethingOnException() {
        fail();
    }

    @Test
    void shouldDoSomethingOnMessagingServiceException() {
        fail();
    }

    @ParameterizedTest
    @MethodSource({"validatorsSource"})
    void shouldCallValidationStrategyFactoryOnUpdate(Update update, CommandType expectedCommandType) {
        when(telegramBot.poll()).thenReturn(update).thenReturn(null);
        doReturn(getCompletableFuturePollMessage()).when(messagingService).sendPollAsync(ArgumentMatchers.any(PollMessageDto.class));

        updateProcessor.process();

        verify(validationStrategyFactory, times(1)).getValidator(eq(expectedCommandType));
    }

    private static Stream<Arguments> validatorsSource() {
        return Stream.of(
                Arguments.of(getTextUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, COMMAND_REFRESH_PROFILE), CommandType.TEXT),
                Arguments.of(getPollAnswerUpdate(), CommandType.POLL_VOTE),
                Arguments.of(getCallbackQueryUpdate(), CommandType.CALLBACK),
                Arguments.of(getFileUploadUpdate(), CommandType.FILE_UPLOAD)
        );
    }

    @ParameterizedTest
    @MethodSource({"commonValidatorSource"})
    void shouldCallCommonCommandMessageValidatorOnUpdate(Update update, CommandMessage expectedCommandMessage) {
        when(telegramBot.poll()).thenReturn(update).thenReturn(null);
        doReturn(getCompletableFuturePollMessage()).when(messagingService).sendPollAsync(ArgumentMatchers.any(PollMessageDto.class));

        updateProcessor.process();

        verify(commonCommandMessageValidator, times(1)).validate(eq(expectedCommandMessage));
    }

    private static Stream<Arguments> commonValidatorSource() {
        Update textUpdate = getTextUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, COMMAND_REFRESH_PROFILE);
        return Stream.of(
                Arguments.of(textUpdate, CommandMessage.getMessageInstance(textUpdate.getMessage())),
                Arguments.of(textUpdate, CommandMessage.getMessageInstance(textUpdate.getMessage())),
                Arguments.of(getFileUploadUpdate(), CommandMessage.getMessageInstance(getFileUploadUpdate().getMessage())),
                Arguments.of(getPollAnswerUpdate(), CommandMessage.getPollAnswerInstance(getPollAnswerUpdate().getPollAnswer())),
                Arguments.of(getCallbackQueryUpdate(), CommandMessage.getCallbackInstance(getCallbackQueryUpdate().getCallbackQuery()))
        );
    }

    @ParameterizedTest
    @MethodSource({"processorsSource"})
    void shouldCallProcessorForUpdate(Update update, Command expectedCommand) {
        when(telegramBot.poll()).thenReturn(update).thenReturn(null);
        doReturn(defaultValidator).when(validationStrategyFactory).getValidator(any());
        doReturn(getCompletableFuturePollMessage()).when(messagingService).sendPollAsync(ArgumentMatchers.any(PollMessageDto.class));

        updateProcessor.process();

        verify(commandProcessorFactory, times(1)).getProcessor(eq(expectedCommand));
    }

    private static Stream<Arguments> processorsSource() {
        return Stream.of(
                Arguments.of(getTextUpdate(TELEGRAM_USER_ID_1, TELEGRAM_CHAT_ID_1, null, COMMAND_REFRESH_PROFILE), Command.REFRESH_PROFILE),
                Arguments.of(getPollAnswerUpdate(), Command.VOTE),
                Arguments.of(getCallbackQueryUpdate(), Command.ACCEPT_SUBMIT),
                Arguments.of(getFileUploadUpdate(), Command.UPLOAD_PHOTO)
        );
    }

    private static Update getTextUpdate(long telegramUserId, long telegramChatId, Integer replyId, String text) {
        User user = new User();
        user.setId(telegramUserId);
        user.setFirstName("newFirstName");
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

    private static Update getPollAnswerUpdate() {
        PollAnswer pollAnswer = new PollAnswer();
        pollAnswer.setUser(getUser());
        pollAnswer.setOptionIds(Collections.singletonList(0));
        pollAnswer.setPollId("100001");
        Update update = new Update();
        update.setPollAnswer(pollAnswer);
        return update;
    }

    private static Update getCallbackQueryUpdate() {
        Message message = new Message();
        message.setMessageId(TELEGRAM_REPLY_ID);
        message.setFrom(getUser());
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData("10000__-1");
        callbackQuery.setFrom(getUser());
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return update;
    }

    private static Update getFileUploadUpdate() {
        Chat chat = new Chat();
        chat.setId(TelegramUpdateProcessorTest.TELEGRAM_CHAT_ID_1);
        chat.setType(ChatType.PRIVATE.getValue());
        Message reply = new Message();
        reply.setMessageId(null);
        Document document = new Document();
        document.setFileId("123");
        document.setFileSize(12345L);
        Message message = new Message();
        message.setMessageId(10000);
        message.setChat(chat);
        message.setFrom(getUser());
        message.setDocument(document);
        message.setReplyToMessage(reply);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private static User getUser() {
        User user = new User();
        user.setId(TelegramUpdateProcessorTest.TELEGRAM_USER_ID_1);
        user.setFirstName("newFirstName");
        return user;
    }

    private CompletableFuture<ExternalPollDto> getCompletableFuturePollMessage() {
        Poll poll = new Poll();
        poll.setId("100001");
        Message message = new Message();
        message.setPoll(poll);
        message.setMessageId(10000);
        Chat chat = new Chat();
        chat.setId(10000L);
        message.setChat(chat);
        CompletableFuture<Message> completableFuture = new CompletableFuture<>();
        completableFuture.complete(message);
        return CompletableFuture.completedFuture(new ExternalPollDto(message));
    }
}
