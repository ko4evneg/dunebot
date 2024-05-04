package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ProfileCommandProcessorTest extends TestContextMock {
    private static final String WRONG_INPUT_EXCEPTION_TEXT =
            "Неверный формат ввода имен. Пример верного формата:" + SettingConstants.EXTERNAL_LINE_SEPARATOR +
            "/profile Иван (Лось) Петров";
    private static final String SUCCESSFUL_UPDATE_MESSAGE = "Данные профиля обновлены\\.";

    private static final Long EXTERNAL_CHAT_ID = 12345L;
    private static final long EXTERNAL_ID = 100500L;

    @Autowired
    private ProfileCommandProcessor processor;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, " +
                             "external_first_name, is_guest, is_chat_blocked, created_at) " +
                             "values (10000, " + EXTERNAL_ID + ", " + EXTERNAL_CHAT_ID + " , 'sn1', 'fn1', 'ln1', 'en1', false, false, '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000 or external_id = '" + EXTERNAL_ID + "'");
    }

    @Test
    void shouldUpdateExternalNamesWhenNoArgumentsProvidedAndUserExists() {
        CommandMessage commandMessage = getCommandMessage("/profile", "newUname");

        processor.process(commandMessage);

        Player actualPlayer = jdbcTemplate
                .queryForObject("select * from players where id = 10000", new BeanPropertyRowMapper<>(Player.class));

        assertThat(actualPlayer)
                .isNotNull()
                .extracting(Player::getExternalFirstName, Player::getExternalName)
                .containsExactly("newEFname", "newUname");
    }

    @Test
    void shouldSendChangeNotificationWhenNoArgumentsProvidedAndUserExists() {
        CommandMessage commandMessage = getCommandMessage("/profile", "newUname");

        processor.process(commandMessage);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(EXTERNAL_CHAT_ID.toString(), SUCCESSFUL_UPDATE_MESSAGE);
    }

    @Test
    void shouldThrowWhenNoArgumentsProvidedAndUserNotExists() {
        jdbcTemplate.execute("delete from players where id = 10000");
        CommandMessage commandMessage = getCommandMessage("/profile", "newUname");

        AnswerableDuneBotException actualException =
                catchThrowableOfType(() -> processor.process(commandMessage), AnswerableDuneBotException.class);

        assertThat(actualException)
                .extracting(AnswerableDuneBotException::getTelegramChatId, AnswerableDuneBotException::getMessage)
                .containsExactly(EXTERNAL_CHAT_ID, WRONG_INPUT_EXCEPTION_TEXT);
    }

    @ParameterizedTest
    @MethodSource(value = "inputNamesSource")
    void shouldUpdateSteamNameWhenArgumentsProvidedAndUserExists(String arguments, String expectedName) {
        processor.process(getCommandMessage("/profile " + arguments, "newUname"));

        String actualName = jdbcTemplate.queryForObject("select steam_name from players where id = 10000", String.class);

        assertThat(actualName).isEqualTo(expectedName);

    }

    private static Stream<Arguments> inputNamesSource() {
        return Stream.of(
                Arguments.of("A (newName) B", "newName"),
                Arguments.of("A (new Name X X) B", "new Name X X")
        );
    }

    @Test
    void shouldSendChangeNotificationWhenArgumentsProvidedAndUserExists() {
        CommandMessage commandMessage = getCommandMessage("/profile a (b) c", "newUname");

        processor.process(commandMessage);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessage = messageCaptor.getValue();

        assertThat(actualMessage)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(EXTERNAL_CHAT_ID.toString(), SUCCESSFUL_UPDATE_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/profile", "/profile a (b) c"})
    void shouldRemoveChatBlockedFlag(String commandText) {
        jdbcTemplate.execute("update players set is_guest = true, is_chat_blocked = true where id = 10000");

        processor.process(getCommandMessage(commandText, "newUname"));

        Boolean actualIsChatBlocked = jdbcTemplate.queryForObject("select is_chat_blocked from players where id = 10000", Boolean.class);

        assertThat(actualIsChatBlocked).isNotNull().isFalse();
    }

    @Test
    void shouldSetGuestNames() {
        jdbcTemplate.execute("update players set is_guest = true where id = 10000");

        processor.process(getCommandMessage("/profile abc (stm) cde", "newUname"));

        Player actualPlayer = jdbcTemplate.queryForObject("select * from players where id = 10000", new BeanPropertyRowMapper<>(Player.class));

        assertThat(actualPlayer)
                .extracting(Player::getFirstName, Player::getLastName, Player::getSteamName)
                .containsExactly("abc", "cde", "stm");
    }

    @Test
    void shouldMigrateGuestPlayerToRegular() {
        jdbcTemplate.execute("update players set is_guest = true where id = 10000");

        processor.process(getCommandMessage("/profile abc (stm) cde", "newUname"));

        Boolean actualIsGuest = jdbcTemplate.queryForObject("select is_guest from players where id = 10000", Boolean.class);

        assertThat(actualIsGuest).isNotNull().isFalse();
    }

    @Test
    void shouldSaveMinimallyFilledNewPlayer() {
        jdbcTemplate.execute("delete from players where id = 10000");
        CommandMessage commandMessage = getCommandMessage("/profile abc (stm) cde", null);

        processor.process(commandMessage);

        Boolean wasPlayerCreated = jdbcTemplate
                .queryForObject("select exists(select 1 from players where first_name = 'abc' and last_name = 'cde' " +
                                "and steam_name = 'stm' and external_first_name = 'newEFname' and external_name is null)", Boolean.class);

        assertThat(wasPlayerCreated).isNotNull().isTrue();
    }

    @Test
    void shouldSaveCompletelyFilledNewPlayer() {
        jdbcTemplate.execute("delete from players where id = 10000");
        CommandMessage commandMessage = getCommandMessage("/profile abc (stm) cde", "EUname");

        processor.process(commandMessage);

        Boolean wasPlayerCreated = jdbcTemplate
                .queryForObject("select exists(select 1 from players where first_name = 'abc' and last_name = 'cde' " +
                                "and steam_name = 'stm' and external_first_name = 'newEFname' and external_name = 'EUname')", Boolean.class);

        assertThat(wasPlayerCreated).isNotNull().isTrue();
    }

    @Test
    void shouldSaveTelegramIdAndTelegramChatIdForNewPlayer() {
        jdbcTemplate.execute("delete from players where id = 10000");

        processor.process(getCommandMessage("/profile abc (stm) cde", "EUname"));

        Player actualPlayer = jdbcTemplate
                .queryForObject("select external_id, external_chat_id from players where first_name = 'abc' and last_name = 'cde' " +
                                "and steam_name = 'stm' and external_first_name = 'newEFname' and external_name = 'EUname'", new BeanPropertyRowMapper<>(Player.class));

        assertThat(actualPlayer).isNotNull()
                .extracting(Player::getExternalId, Player::getExternalChatId)
                .containsExactly(EXTERNAL_ID, EXTERNAL_CHAT_ID);
    }

    @Test
    void shouldSaveNewPlayerWithMultipleWordsSteamName() {
        jdbcTemplate.execute("delete from players where id = 10000");

        processor.process(getCommandMessage("/profile abc (Vasiliy Prostoy V) cde", "uName"));

        String actualSteamName = jdbcTemplate
                .queryForObject("select steam_name from players where external_id = '" + EXTERNAL_ID + "'", String.class);

        assertThat(actualSteamName).isEqualTo("Vasiliy Prostoy V");
    }

    @Test
    void shouldThrowWhenSameSteamNameUserExists() {
        jdbcTemplate.execute("update players set external_id = 11111 where id = 10000");

        CommandMessage commandMessage = getCommandMessage("/profile a (sn1) b", "uName");

        assertThatThrownBy(() -> processor.process(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Пользователь со steam ником 'sn1' уже существует!");
    }

    @Test
    void shouldNotThrowWhenSameSteamNameBelongsRequestingUser() {
        CommandMessage commandMessage = getCommandMessage("/profile a (sn1) b", "uName");

        assertThatCode(() -> processor.process(commandMessage)).doesNotThrowAnyException();
    }

    @Test
    void shouldSendMessageOnNewUserRegistration() {
        jdbcTemplate.execute("delete from players where id = 10000");

        processor.process(getCommandMessage("/profile a (sn1) b", "uName"));

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessages = messageCaptor.getValue();

        assertThat(actualMessages).isNotNull()
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(EXTERNAL_CHAT_ID.toString(), "Вы зарегистрированы как 'a \\(sn1\\) b'");
    }

    @Test
    void shouldReturnProfileCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.PROFILE);
    }

    private static CommandMessage getCommandMessage(String commandText, @Nullable String userName) {
        User user = new User();
        user.setId(EXTERNAL_ID);
        user.setUserName(userName);
        user.setFirstName("newEFname");
        Chat chat = new Chat();
        chat.setId(EXTERNAL_CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(10000);
        message.setFrom(user);
        message.setChat(chat);
        message.setText(commandText);
        return CommandMessage.getMessageInstance(message);
    }
}
