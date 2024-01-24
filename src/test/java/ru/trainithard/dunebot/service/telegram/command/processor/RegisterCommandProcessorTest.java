package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.lang.Nullable;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDuneBotException;
import ru.trainithard.dunebot.model.Player;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class RegisterCommandProcessorTest extends TestContextMock {
    @Autowired
    private RegisterCommandProcessor commandProcessor;

    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final Long TELEGRAM_CHAT_ID = 9000L;
    private static final String FIRST_NAME = "fName";
    private static final String STEAM_NAME = "stName";
    private static final String REGISTRATION_MESSAGE = "Вы зарегистрированы под steam-именем " + STEAM_NAME;
    private final CommandMessage commandMessage = getCommandMessage(STEAM_NAME, null, null);

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000 or external_id = '" + TELEGRAM_USER_ID + "'");
    }

    @Test
    void shouldSaveMinimallyFilledNewPlayer() {
        commandProcessor.process(commandMessage);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select count(*) from players " +
                "where first_name = '" + FIRST_NAME + "' and steam_name = '" + STEAM_NAME + "'", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldSaveTelegramIdAndTelegramIdForNewPlayer() {
        commandProcessor.process(commandMessage);

        Player actualPlayer = jdbcTemplate.queryForObject("select external_id, external_chat_id from players " +
                "where first_name = '" + FIRST_NAME + "' and steam_name = '" + STEAM_NAME + "'", new BeanPropertyRowMapper<>(Player.class));

        assertNotNull(actualPlayer);
        assertEquals(TELEGRAM_USER_ID, actualPlayer.getExternalId());
        assertEquals(TELEGRAM_CHAT_ID, actualPlayer.getExternalChatId());
    }

    @Test
    void shouldSaveCompletelyFilledNewPlayer() {
        commandProcessor.process(getCommandMessage(STEAM_NAME, "lName", "uName"));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select count(*) from players " +
                "where first_name = '" + FIRST_NAME + "' and steam_name = '" + STEAM_NAME + "'" +
                "and last_name = 'lName' and external_name = 'uName'", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldSaveNewPlayerWithMultipleWordsSteamName() {
        commandProcessor.process(getCommandMessage("Vasiliy Prostoy V", "lName", "uName"));

        String actualSteamName = jdbcTemplate.queryForObject("select steam_name from players " +
                "where first_name = '" + FIRST_NAME + "' and last_name = 'lName' and external_name = 'uName'", String.class);

        assertEquals("Vasiliy Prostoy V", actualSteamName);
    }

    @Test
    void shouldThrowWhenSameTelegramIdUserExists() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + TELEGRAM_USER_ID + ", " + TELEGRAM_CHAT_ID + ", '" + STEAM_NAME + "', '" + FIRST_NAME + "', '2000-10-10')");

        AnswerableDuneBotException exception = assertThrows(AnswerableDuneBotException.class, () -> commandProcessor.process(commandMessage));
        assertEquals("Вы уже зарегистрированы под steam ником " + STEAM_NAME + "! Для смены ника выполните команду '/change_steam_name *new_name*'", exception.getMessage());
    }

    @Test
    void shouldThrowWhenSameSteamNameUserExists() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, created_at) " +
                "values (10000, " + (TELEGRAM_USER_ID + 1) + ", " + (TELEGRAM_CHAT_ID + 1) + ", '" + STEAM_NAME + "', '" + FIRST_NAME + "', '2000-10-10')");

        AnswerableDuneBotException exception = assertThrows(AnswerableDuneBotException.class, () -> commandProcessor.process(commandMessage));
        assertEquals("Пользователь со steam ником " + STEAM_NAME + " уже существует!", exception.getMessage());
    }

    @Test
    void shouldSendTelegramMessageOnUserRegistration() {
        commandProcessor.process(commandMessage);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessages = messageCaptor.getValue();

        assertEquals(TELEGRAM_CHAT_ID.toString(), actualMessages.getChatId());
        assertEquals(REGISTRATION_MESSAGE, actualMessages.getText());
    }

    private CommandMessage getCommandMessage(String steamName, @Nullable String lastName, @Nullable String userName) {
        User user = new User();
        user.setId(TELEGRAM_USER_ID);
        user.setFirstName(FIRST_NAME);
        user.setLastName(lastName);
        user.setUserName(userName);
        Chat chat = new Chat();
        chat.setId(TELEGRAM_CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(10000);
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/register " + steamName);
        return CommandMessage.getMessageInstance(message);
    }
}
