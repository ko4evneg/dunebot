package ru.trainithard.dunebot.service.telegram.factory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

class CommandMessageFactoryImplTest {
    private static final long USER_ID = 100L;
    private static final long CHAT_ID = 200L;
    private static final int REPLY_ID = 300;
    private static final int MESSAGE_ID = 400;
    private static final String POLL_ID = "100001";
    private static final String CALLBACK_DATA = "10000__" + NOT_PARTICIPATED_MATCH_PLACE;
    private final CommandMessageFactoryImpl factory = new CommandMessageFactoryImpl();

    @Test
    void shouldCreateCommandMessageForSlashTextUpdate() {
        Update textUpdate = getTextUpdate("/anywordwithslashprefix");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertNotNull(commandMessage);
        assertThat(commandMessage, allOf(
                hasProperty("userId", is(USER_ID)),
                hasProperty("externalFirstName", is("fName")),
                hasProperty("userName", is("uName")),
                hasProperty("chatId", is(CHAT_ID)),
                hasProperty("messageId", is(MESSAGE_ID)),
                hasProperty("replyMessageId", is(REPLY_ID)),
                hasProperty("command", nullValue())
        ));
    }

    @ParameterizedTest
    @EnumSource(value = Command.class, mode = EnumSource.Mode.EXCLUDE, names = {"VOTE", "ACCEPT_SUBMIT"})
    void shouldGetValidCommandInCommandMessageForSlashTextUpdate(Command command) {
        Update textUpdate = getTextUpdate("/" + command + " randomArg");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertEquals(command, commandMessage.getCommand());
        assertEquals(1, commandMessage.getArgumentsCount());
    }

    @Test
    void shouldGetEmptyStringCommandArgsForSlashWordTextUpdate() {
        Update textUpdate = getTextUpdate("/" + Command.REGISTER);

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertTrue(commandMessage.getAllArguments().isBlank());
        assertEquals(0, commandMessage.getArgumentsCount());
    }

    @Test
    void shouldGetAllCommandArgsForSlashMultipleWordTextUpdate() {
        Update textUpdate = getTextUpdate("/" + Command.REGISTER + " arg1 arg2 arg3");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertEquals("arg1 arg2 arg3", commandMessage.getAllArguments());
        assertEquals(3, commandMessage.getArgumentsCount());
        assertEquals("arg3", commandMessage.getArgument(3));
        assertEquals("arg1", commandMessage.getArgument(1));
        assertNull(commandMessage.getArgument(0));
        assertNull(commandMessage.getArgument(4));
    }

    @Test
    void shouldGetNoCommandArgsForSlashMultipleWordTextUpdate() {
        Update textUpdate = getTextUpdate("/" + Command.REGISTER + " arg1 arg2 arg3");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertEquals("arg1 arg2 arg3", commandMessage.getAllArguments());
        assertEquals(3, commandMessage.getArgumentsCount());
        assertEquals("arg2", commandMessage.getArgument(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"text", "two words", "", "register"})
    void shouldGetNoCommandArgsForNoSlashTextUpdate(String text) {
        Update textUpdate = getTextUpdate(text);

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertNull(commandMessage);
    }

    @Test
    void shouldReturnNullForMessageUpdateWithoutText() {
        Update textUpdate = getTextUpdate(null);

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertNull(commandMessage);
    }

    @Test
    void shouldReturnNullForUnknownTypeUpdate() {
        CommandMessage commandMessage = factory.getInstance(new Update());

        assertNull(commandMessage);
    }

    @Test
    void shouldCreateCommandMessageForPollAnswerCommandUpdate() {
        Update pollAnswerUpdate = getPollAnswerUpdate();

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);
        CommandMessage.PollVote pollVote = commandMessage.getPollVote();

        assertNotNull(commandMessage);
        assertEquals(USER_ID, commandMessage.getUserId());
        assertEquals(Command.VOTE, commandMessage.getCommand());
        assertEquals(POLL_ID, pollVote.pollId());
        assertThat(pollVote.selectedAnswerId(), contains(0));
    }

    @Test
    void shouldCreateCommandMessageForPollAnswerWithoutOptionsCommandUpdate() {
        Update pollAnswerUpdate = getPollAnswerUpdate();
        pollAnswerUpdate.getPollAnswer().setOptionIds(Collections.emptyList());

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);
        CommandMessage.PollVote pollVote = commandMessage.getPollVote();

        assertNotNull(commandMessage);
        assertEquals(USER_ID, commandMessage.getUserId());
        assertEquals(Command.VOTE, commandMessage.getCommand());
        assertEquals(POLL_ID, pollVote.pollId());
        assertTrue(pollVote.selectedAnswerId().isEmpty());
    }

    @Test
    void shouldCreateCommandMessageForCallbackCommandUpdate() {
        Update pollAnswerUpdate = getCallbackQueryUpdate();

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);

        assertNotNull(commandMessage);
        assertEquals(USER_ID, commandMessage.getUserId());
        assertEquals(Command.ACCEPT_SUBMIT, commandMessage.getCommand());
        assertEquals(CALLBACK_DATA, commandMessage.getCallback());
    }

    @Test
    void shouldCreateNullCommandMessageForNullCallbackCommandUpdate() {
        Update pollAnswerUpdate = getCallbackQueryUpdate();
        pollAnswerUpdate.getCallbackQuery().setData(null);

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);

        assertNull(commandMessage);
    }

    @Test
    void shouldCreateNullCommandMessageForEmptyCallbackCommandUpdate() {
        Update pollAnswerUpdate = getCallbackQueryUpdate();
        pollAnswerUpdate.getCallbackQuery().setData("");

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);

        assertNull(commandMessage);
    }

    private static Update getTextUpdate(String text) {
        User user = new User();
        user.setId(USER_ID);
        user.setFirstName("fName");
        user.setUserName("uName");
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message reply = new Message();
        reply.setMessageId(REPLY_ID);
        Message message = new Message();
        message.setMessageId(MESSAGE_ID);
        message.setChat(chat);
        message.setFrom(user);
        message.setText(text);
        message.setReplyToMessage(reply);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private static Update getPollAnswerUpdate() {
        User user = new User();
        user.setId(USER_ID);
        PollAnswer pollAnswer = new PollAnswer();
        pollAnswer.setUser(user);
        pollAnswer.setOptionIds(Collections.singletonList(0));
        pollAnswer.setPollId(POLL_ID);
        Update update = new Update();
        update.setPollAnswer(pollAnswer);
        return update;
    }

    private static Update getCallbackQueryUpdate() {
        User user = new User();
        user.setId(USER_ID);
        Message message = new Message();
        message.setMessageId(MESSAGE_ID);
        message.setFrom(user);
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData(CALLBACK_DATA);
        callbackQuery.setFrom(user);
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return update;
    }
}
