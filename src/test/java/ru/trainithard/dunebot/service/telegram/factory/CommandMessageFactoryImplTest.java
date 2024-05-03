package ru.trainithard.dunebot.service.telegram.factory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import ru.trainithard.dunebot.model.Match;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.repository.MatchRepository;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.trainithard.dunebot.configuration.SettingConstants.NOT_PARTICIPATED_MATCH_PLACE;

class CommandMessageFactoryImplTest {
    private static final long USER_ID = 100L;
    private static final long CHAT_ID = 200L;
    private static final int REPLY_ID = 300;
    private static final int MESSAGE_ID = 400;
    private static final String POLL_ID = "100001";
    private static final String ACCEPT_SUBMIT_CALLBACK_DATA = "10000__" + NOT_PARTICIPATED_MATCH_PLACE;
    private static final String LEADER_CALLBACK_DATA = "10000_L_2";
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final CommandMessageFactoryImpl factory = new CommandMessageFactoryImpl(matchRepository);

    @Test
    void shouldCreateCommandMessageForSlashTextUpdate() {
        Update textUpdate = getTextUpdate("/anywordwithslashprefix");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertThat(commandMessage).isNotNull()
                .extracting("userId", "externalFirstName", "userName",
                        "chatId", "messageId", "replyMessageId", "command")
                .containsExactly(USER_ID, "fName", "uName",
                        CHAT_ID, MESSAGE_ID, REPLY_ID, null);
    }

    @ParameterizedTest
    @EnumSource(value = Command.class, mode = EnumSource.Mode.EXCLUDE, names = {"VOTE", "ACCEPT_SUBMIT"})
    void shouldGetValidCommandInCommandMessageForSlashTextUpdate(Command command) {
        Update textUpdate = getTextUpdate("/" + command + " randomArg");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertThat(commandMessage.getCommand()).isEqualTo(command);
        assertThat(commandMessage.getArgumentsCount()).isEqualTo(1);
    }

    @Test
    void shouldIgnoreExcessiveSpacesBetweenArguments() {
        Update textUpdate = getTextUpdate("/" + Command.REGISTER + "   arg1  arg2   arg3    ");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertThat(commandMessage.getArgumentsCount()).isEqualTo(3);
        assertThat(commandMessage.getAllArguments()).isEqualTo("arg1 arg2 arg3");
    }

    @Test
    void shouldGetEmptyStringCommandArgsForSlashWordTextUpdate() {
        Update textUpdate = getTextUpdate("/" + Command.REGISTER);

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertThat(commandMessage.getAllArguments()).isBlank();
        assertThat(commandMessage.getArgumentsCount()).isZero();
    }

    @Test
    void shouldGetAllCommandArgsForSlashMultipleWordTextUpdate() {
        Update textUpdate = getTextUpdate("/" + Command.REGISTER + " arg1 arg2 arg3");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertThat(commandMessage.getAllArguments()).isEqualTo("arg1 arg2 arg3");
        assertThat(commandMessage.getArgumentsCount()).isEqualTo(3);
        assertThat(commandMessage.getArgument(3)).isEqualTo("arg3");
        assertThat(commandMessage.getArgument(1)).isEqualTo("arg1");
        assertThat(commandMessage.getArgument(0)).isNull();
        assertThat(commandMessage.getArgument(4)).isNull();
    }

    @Test
    void shouldGetNoCommandArgsForSlashMultipleWordTextUpdate() {
        Update textUpdate = getTextUpdate("/" + Command.REGISTER + " arg1 arg2 arg3");

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertThat(commandMessage.getAllArguments()).isEqualTo("arg1 arg2 arg3");
        assertThat(commandMessage.getArgumentsCount()).isEqualTo(3);
        assertThat(commandMessage.getArgument(2)).isEqualTo("arg2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"text", "two words", "", "register"})
    void shouldGetNoCommandArgsForNoSlashTextUpdate(String text) {
        Update textUpdate = getTextUpdate(text);

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertThat(commandMessage).isNull();
    }

    @Test
    void shouldReturnNullForMessageUpdateWithoutText() {
        Update textUpdate = getTextUpdate(null);

        CommandMessage commandMessage = factory.getInstance(textUpdate);

        assertThat(commandMessage).isNull();
    }

    @Test
    void shouldReturnNullForUnknownTypeUpdate() {
        CommandMessage commandMessage = factory.getInstance(new Update());

        assertThat(commandMessage).isNull();
    }

    @Test
    void shouldCreateCommandMessageForPollAnswerCommandUpdateWhenMatchWithSamePollIdExists() {
        doReturn(Optional.of(new Match())).when(matchRepository).findByExternalPollIdPollId(POLL_ID);
        Update pollAnswerUpdate = getPollAnswerUpdate();

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);

        assertThat(commandMessage).isNotNull()
                .extracting(CommandMessage::getUserId, CommandMessage::getCommand)
                .containsExactly(USER_ID, Command.VOTE);

        CommandMessage.PollVote pollVote = commandMessage.getPollVote();
        assertThat(pollVote.pollId()).isEqualTo(POLL_ID);
        assertThat(pollVote.selectedAnswerId()).containsExactly(0);
    }

    @Test
    void shouldNotCreateCommandMessageForPollAnswerCommandUpdateWhenNoMatchWithSamePollIdExists() {
        doReturn(Optional.empty()).when(matchRepository).findByExternalPollIdPollId(POLL_ID);
        Update pollAnswerUpdate = getPollAnswerUpdate();

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);

        assertThat(commandMessage).isNull();
    }

    @Test
    void shouldCreateCommandMessageForPollAnswerWithoutOptionsCommandUpdate() {
        doReturn(Optional.of(new Match())).when(matchRepository).findByExternalPollIdPollId(POLL_ID);
        Update pollAnswerUpdate = getPollAnswerUpdate();
        pollAnswerUpdate.getPollAnswer().setOptionIds(Collections.emptyList());

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);

        assertThat(commandMessage).isNotNull()
                .extracting(CommandMessage::getUserId, CommandMessage::getCommand)
                .containsExactly(USER_ID, Command.VOTE);

        CommandMessage.PollVote pollVote = commandMessage.getPollVote();
        assertThat(pollVote.pollId()).isEqualTo(POLL_ID);
        assertThat(pollVote.selectedAnswerId()).isEmpty();
    }

    @Test
    void shouldCreateCommandMessageForSubmitCallbackCommandUpdate() {
        Update callbackReplyUpdate = getCallbackQueryUpdate(ACCEPT_SUBMIT_CALLBACK_DATA);

        CommandMessage commandMessage = factory.getInstance(callbackReplyUpdate);

        assertThat(commandMessage).isNotNull()
                .extracting(CommandMessage::getUserId, CommandMessage::getCommand, CommandMessage::getCallback)
                .containsExactly(USER_ID, Command.ACCEPT_SUBMIT, ACCEPT_SUBMIT_CALLBACK_DATA);
    }

    @Test
    void shouldCreateCommandMessageForLeaderCallbackCommandUpdate() {
        Update callbackReplyUpdate = getCallbackQueryUpdate(LEADER_CALLBACK_DATA);

        CommandMessage commandMessage = factory.getInstance(callbackReplyUpdate);

        assertThat(commandMessage).isNotNull()
                .extracting(CommandMessage::getUserId, CommandMessage::getCommand, CommandMessage::getCallback)
                .containsExactly(USER_ID, Command.LEADER, LEADER_CALLBACK_DATA);
    }

    @Test
    void shouldCreateNullCommandMessageForNullCallbackCommandUpdate() {
        Update pollAnswerUpdate = getCallbackQueryUpdate(ACCEPT_SUBMIT_CALLBACK_DATA);
        pollAnswerUpdate.getCallbackQuery().setData(null);

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);

        assertThat(commandMessage).isNull();
    }

    @Test
    void shouldCreateNullCommandMessageForEmptyCallbackCommandUpdate() {
        Update pollAnswerUpdate = getCallbackQueryUpdate(ACCEPT_SUBMIT_CALLBACK_DATA);
        pollAnswerUpdate.getCallbackQuery().setData("");

        CommandMessage commandMessage = factory.getInstance(pollAnswerUpdate);

        assertThat(commandMessage).isNull();
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

    private static Update getCallbackQueryUpdate(String callbackData) {
        User user = new User();
        user.setId(USER_ID);
        Message message = new Message();
        message.setMessageId(MESSAGE_ID);
        message.setFrom(user);
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setMessage(message);
        callbackQuery.setData(callbackData);
        callbackQuery.setFrom(user);
        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return update;
    }
}
