package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class RefreshProfileCommandProcessorTest extends TestContextMock {
    private static final Long CHAT_ID = 12345L;
    private static final long USER_ID = 12345L;
    private static final String SUCCESSFUL_UPDATE_MESSAGE = "Данные профиля обновлены\\.";

    @Autowired
    private RefreshProfileCommandProcessor processor;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_name, external_first_name, created_at) " +
                             "values (10000, " + USER_ID + ", " + CHAT_ID + ", 'st_pl', 'oldFname', 'oldLname', 'oldUname', 'oldEFname', '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @ParameterizedTest
    @MethodSource(value = "inputNamesSource")
    void shouldChangeSteamName(String newName, String expectedName) {
        processor.process(getCommandMessage(newName));

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
    void shouldNotChangeSteamNameWhenNoNewNameProvided() {
        processor.process(getCommandMessage(""));

        Boolean isChangedUserExist = jdbcTemplate.queryForObject("select exists(select 1 from players where id = 10000 " +
                                                                 "and steam_name = 'st_pl')", Boolean.class);

        assertThat(isChangedUserExist).isNotNull().isTrue();
    }

    @Test
    void shouldChangeProvidedNames() {
        processor.process(getCommandMessage("abc (stm) cde"));

        Boolean isChangedUserExist = jdbcTemplate.queryForObject("select exists(select 1 from players where id = 10000 " +
                                                                 "and first_name = 'abc' and last_name = 'cde' and steam_name = 'stm')", Boolean.class);

        assertThat(isChangedUserExist).isNotNull().isTrue();
    }

    @Test
    void shouldSendMessageOnProvidedNamesChange() {
        processor.process(getCommandMessage("abc (stm) cde"));

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();


        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(CHAT_ID.toString(), SUCCESSFUL_UPDATE_MESSAGE);
    }

    @Test
    void shouldChangeTelegramFirstName() {
        processor.process(getCommandMessage("abc (stm) cde"));

        Boolean isChangedUserExist = jdbcTemplate.queryForObject("select exists(select 1 from players where id = 10000 " +
                                                                 "and external_first_name = 'newEFname' and external_name = 'newUname')", Boolean.class);

        assertThat(isChangedUserExist).isNotNull().isTrue();
    }

    @Test
    void shouldChangeTelegramFirstNameWhenNoArgsProvided() {
        processor.process(getCommandMessage(""));

        Boolean isChangedUserExist = jdbcTemplate.queryForObject("select exists(select 1 from players where id = 10000 " +
                                                                 "and external_first_name = 'newEFname' and external_name = 'newUname')", Boolean.class);

        assertThat(isChangedUserExist).isNotNull().isTrue();
    }

    @Test
    void shouldReturnRefreshCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.REFRESH_PROFILE);
    }

    @Test
    void shouldSetGuestNames() {
        jdbcTemplate.execute("update players set is_guest = true where id = 10000");

        processor.process(getCommandMessage("abc (stm) cde"));

        Player actualPlayer = jdbcTemplate.queryForObject("select * from players where id = 10000", new BeanPropertyRowMapper<>(Player.class));

        assertThat(actualPlayer)
                .extracting(Player::getFirstName, Player::getLastName, Player::getSteamName)
                .containsExactly("abc", "cde", "stm");
    }

    @Test
    void shouldMigrateGuestPlayerToRegular() {
        jdbcTemplate.execute("update players set is_guest = true where id = 10000");

        processor.process(getCommandMessage("abc (stm) cde"));

        Boolean actualIsGuest = jdbcTemplate.queryForObject("select is_guest from players where id = 10000", Boolean.class);

        assertThat(actualIsGuest).isNotNull().isFalse();
    }

    private CommandMessage getCommandMessage(String newSteamName) {
        Message message = new Message();
        message.setMessageId(10000);
        message.setText("/refresh_profile " + newSteamName);
        Chat chat = new Chat();
        chat.setId(CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        message.setChat(chat);
        User user = new User();
        user.setId(USER_ID);
        user.setFirstName("newEFname");
        user.setUserName("newUname");
        message.setFrom(user);
        return CommandMessage.getMessageInstance(message);
    }
}
