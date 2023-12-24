package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.exception.AnswerableDubeBotException;
import ru.trainithard.dunebot.service.dto.PlayerRegistrationDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PlayerServiceTest extends TestContextMock {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TextCommandProcessor textCommandProcessor;

    private static final Long TELEGRAM_USER_ID = 12345L;
    private static final String FIRST_NAME = "fName";
    private static final String STEAM_NAME = "stName";

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from players where id = 10000 or telegram_id = '" + TELEGRAM_USER_ID + "'");
    }

    @Test
    void shouldCreateMinimallyFilledNewPlayer() {
        PlayerRegistrationDto playerRegistration = getPlayerRegistrationDto(null, null);

        textCommandProcessor.registerNewPlayer(playerRegistration);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select count(*) from players " +
                "where first_name = '" + FIRST_NAME + "' and steam_name = '" + STEAM_NAME + "'", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldCreateCompletelyFilledNewPlayer() {
        PlayerRegistrationDto playerRegistration = getPlayerRegistrationDto("lName", "uName");

        textCommandProcessor.registerNewPlayer(playerRegistration);

        Long actualPlayersCount = jdbcTemplate.queryForObject("select count(*) from players " +
                "where first_name = '" + FIRST_NAME + "' and steam_name = '" + STEAM_NAME + "'" +
                "and last_name = 'lName' and user_name = 'uName'", Long.class);

        assertEquals(1, actualPlayersCount);
    }

    @Test
    void shouldThrowWhenSameTelegramIdUserExists() {
        jdbcTemplate.execute("insert into players (id, telegram_id, steam_name, first_name, created_at) " +
                "values (10000, " + TELEGRAM_USER_ID + ", '" + STEAM_NAME + "', '" + FIRST_NAME + "', '2000-10-10')");
        PlayerRegistrationDto playerRegistration = getPlayerRegistrationDto(null, null);

        AnswerableDubeBotException exsception = assertThrows(AnswerableDubeBotException.class, () -> textCommandProcessor.registerNewPlayer(playerRegistration));
        assertEquals("Вы уже зарегистрированы под steam ником " + STEAM_NAME + "!", exsception.getMessage());
    }

    @Test
    void shouldThrowWhenSameSteamNameUserExists() {
        jdbcTemplate.execute("insert into players (id, telegram_id, steam_name, first_name, created_at) " +
                "values (10000, " + (TELEGRAM_USER_ID + 1) + ", '" + STEAM_NAME + "', '" + FIRST_NAME + "', '2000-10-10')");
        PlayerRegistrationDto playerRegistration = getPlayerRegistrationDto(null, null);

        AnswerableDubeBotException exsception = assertThrows(AnswerableDubeBotException.class, () -> textCommandProcessor.registerNewPlayer(playerRegistration));
        assertEquals("Пользователь со steam ником " + STEAM_NAME + " уже существует!", exsception.getMessage());
    }

    private PlayerRegistrationDto getPlayerRegistrationDto(String lastName, String steamName) {
        User user = new User();
        user.setId(TELEGRAM_USER_ID);
        user.setFirstName(FIRST_NAME);
        user.setLastName(lastName);
        user.setUserName(steamName);
        Message message = new Message();
        message.setFrom(user);
        return new PlayerRegistrationDto(message, STEAM_NAME);
    }
}
