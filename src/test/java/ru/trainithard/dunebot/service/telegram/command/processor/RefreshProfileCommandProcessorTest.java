package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RefreshProfileCommandProcessorTest extends TestContextMock {
    @Autowired
    private RefreshProfileCommandProcessor commandProcessor;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_name, created_at) " +
                "values (10000, 12345, 9000, 'st_pl', 'oldFname', 'oldLname','oldUname','2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"newName", "new Name X X"})
    void shouldChangeSteamName(String newName) {
        commandProcessor.process(getCommandMessage(newName));

        String actualName = jdbcTemplate.queryForObject("select steam_name from players where id = 10000", String.class);

        assertEquals(newName, actualName);
    }

    @Test
    void shouldNotChangeSteamNameWhenNoNewNameProvided() {
        commandProcessor.process(getCommandMessage(""));

        Boolean isChangedUserExist = jdbcTemplate.queryForObject("select exists(select 1 from players where id = 10000 " +
                "and steam_name = 'st_pl')", Boolean.class);

        assertNotNull(isChangedUserExist);
        assertTrue(isChangedUserExist);
    }

    @Test
    void shouldChangeTelegramNames() {
        commandProcessor.process(getCommandMessage(""));

        Boolean isChangedUserExist = jdbcTemplate.queryForObject("select exists(select 1 from players where id = 10000 " +
                "and first_name = 'newFname' and last_name = 'newLname' and external_name = 'newUname')", Boolean.class);

        assertNotNull(isChangedUserExist);
        assertTrue(isChangedUserExist);
    }

    private CommandMessage getCommandMessage(String newSteamName) {
        Message message = new Message();
        message.setMessageId(10000);
        message.setText("/change_steam_name " + newSteamName);
        Chat chat = new Chat();
        chat.setId(9000L);
        chat.setType(ChatType.PRIVATE.getValue());
        message.setChat(chat);
        User user = new User();
        user.setId(12345L);
        user.setFirstName("newFname");
        user.setLastName("newLname");
        user.setUserName("newUname");
        message.setFrom(user);
        return CommandMessage.getMessageInstance(message);
    }
}
