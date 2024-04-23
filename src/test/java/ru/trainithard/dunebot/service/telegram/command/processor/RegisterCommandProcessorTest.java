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
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class RegisterCommandProcessorTest extends TestContextMock {
    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final Long TELEGRAM_CHAT_ID = 9000L;
    private static final String FIRST_NAME = "fName";
    private static final String LAST_NAME = "lName";
    private static final String STEAM_NAME = "stName";
    private static final String REGISTRATION_MESSAGE =
            String.format("Вы зарегистрированы как '%s \\(%s\\) %s'", FIRST_NAME, STEAM_NAME, LAST_NAME);
    private static final CommandMessage commandMessage = getCommandMessage(STEAM_NAME, null);
    private static final String EXTERNAL_FIRST_NAME = "extName";

    @Autowired
    private RegisterCommandProcessor processor;

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000 or external_id = '" + TELEGRAM_USER_ID + "'");
    }

    @Test
    void shouldSaveMinimallyFilledNewPlayer() {
        processor.process(commandMessage);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select count(*) from players " +
                                                              "where first_name = '" + FIRST_NAME + "' and last_name = '" + LAST_NAME + "' " +
                                                              "and steam_name = '" + STEAM_NAME + "' and external_first_name = '" + EXTERNAL_FIRST_NAME + "'", Long.class);

        assertThat(actualPlayersCount).isEqualTo(1);
    }

    @Test
    void shouldSaveCompletelyFilledNewPlayer() {
        processor.process(getCommandMessage(STEAM_NAME, "uName"));

        Long actualPlayersCount = jdbcTemplate.queryForObject("select count(*) from players " +
                                                              "where first_name = '" + FIRST_NAME + "' and steam_name = '" + STEAM_NAME + "' and last_name = '" +
                                                              LAST_NAME + "' and external_name = 'uName' and external_first_name = '" + EXTERNAL_FIRST_NAME + "'", Long.class);

        assertThat(actualPlayersCount).isEqualTo(1);
    }

    @Test
    void shouldSaveTelegramIdAndTelegramChatIdForNewPlayer() {
        processor.process(commandMessage);

        Player actualPlayer = jdbcTemplate.queryForObject("select external_id, external_chat_id from players " +
                                                          "where first_name = '" + FIRST_NAME + "' and steam_name = '" + STEAM_NAME + "'", new BeanPropertyRowMapper<>(Player.class));

        assertThat(actualPlayer).isNotNull()
                .extracting(Player::getExternalId, Player::getExternalChatId)
                .containsExactly(TELEGRAM_USER_ID, TELEGRAM_CHAT_ID);
    }

    @Test
    void shouldSaveNewPlayerWithMultipleWordsSteamName() {
        processor.process(getCommandMessage("Vasiliy Prostoy V", "uName"));

        String actualSteamName = jdbcTemplate.queryForObject("select steam_name from players " +
                                                             "where first_name = '" + FIRST_NAME + "' and last_name = 'lName' and external_name = 'uName'", String.class);

        assertThat(actualSteamName).isEqualTo("Vasiliy Prostoy V");
    }

    @Test
    void shouldThrowWhenSameTelegramIdUserExists() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + TELEGRAM_USER_ID + ", " + TELEGRAM_CHAT_ID + ", '" + STEAM_NAME + "', '" +
                             FIRST_NAME + "', 'lName', 'efName', '2000-10-10')");

        assertThatThrownBy(() -> processor.process(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Вы уже зарегистрированы под steam ником " + STEAM_NAME +
                            "! Для смены ника выполните команду '/refresh_profile Имя (ник в steam) Фамилия'");
    }

    @Test
    void shouldThrowWhenSameSteamNameUserExists() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, " + (TELEGRAM_USER_ID + 1) + ", " + (TELEGRAM_CHAT_ID + 1) + ", '" + STEAM_NAME + "', '" +
                             FIRST_NAME + "', 'lName', 'efName', '2000-10-10')");

        assertThatThrownBy(() -> processor.process(commandMessage))
                .isInstanceOf(AnswerableDuneBotException.class)
                .hasMessage("Пользователь со steam ником " + STEAM_NAME + " уже существует!");
    }

    @Test
    void shouldSendTelegramMessageOnUserRegistration() {
        processor.process(commandMessage);

        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageCaptor.capture());
        MessageDto actualMessages = messageCaptor.getValue();

        assertThat(actualMessages).isNotNull()
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly(TELEGRAM_CHAT_ID.toString(), REGISTRATION_MESSAGE);
    }

    @Test
    void shouldReturnRegisterCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.REGISTER);
    }

    private static CommandMessage getCommandMessage(String steamName, @Nullable String userName) {
        User user = new User();
        user.setId(TELEGRAM_USER_ID);
        user.setUserName(userName);
        user.setFirstName(EXTERNAL_FIRST_NAME);
        Chat chat = new Chat();
        chat.setId(TELEGRAM_CHAT_ID);
        chat.setType(ChatType.PRIVATE.getValue());
        Message message = new Message();
        message.setMessageId(10000);
        message.setFrom(user);
        message.setChat(chat);
        message.setText(String.format("/register %s (%s) %s", FIRST_NAME, steamName, LAST_NAME));
        return CommandMessage.getMessageInstance(message);
    }
}
