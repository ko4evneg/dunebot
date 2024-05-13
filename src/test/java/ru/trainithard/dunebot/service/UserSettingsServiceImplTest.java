package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.UserSetting;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.trainithard.dunebot.model.UserSettingKey.HOST;

@SpringBootTest
class UserSettingsServiceImplTest extends TestContextMock {
    @Autowired
    private UserSettingsServiceImpl userSettingsService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, 12345, 9000 , 'st_pl1', 'name1', 'ln1', 'en1', '2010-10-10') ");
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10001, 12346, 9001 , 'st_pl2', 'name2', 'ln2', 'en2', '2010-10-10') ");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from user_settings where player_id between 10000 and 10001");
        jdbcTemplate.execute("delete from players where id between 10000 and 10001");
    }

    @Test
    void shouldGetSpecificSetting() {
        jdbcTemplate.execute("insert into user_settings (id, player_id, key, value, created_at) " +
                             "values (10000, 10000, '" + HOST + "', 'abc/123', '2010-10-10')");

        UserSetting actualSetting = userSettingsService.getSetting(10000L, HOST).orElseThrow();

        assertThat(actualSetting.getValue()).isEqualTo("abc/123");
    }

    @Test
    void shouldGetAllUserSettings() {
        jdbcTemplate.execute("insert into user_settings (id, player_id, key, value, created_at) " +
                             "values (10000, 10000, '" + HOST + "', 'abc/123', '2010-10-10')");
        jdbcTemplate.execute("insert into user_settings (id, player_id, key, value, created_at) " +
                             "values (10001, 10001, '" + HOST + "', 'ooo/222', '2010-10-10')");

        List<UserSetting> actualSettings = userSettingsService.getAllSettings(10000L);

        assertThat(actualSettings).extracting(UserSetting::getValue).containsExactly("abc/123");
    }

    @Test
    void shouldSaveChangedSetting() {
        jdbcTemplate.execute("insert into user_settings (id, player_id, key, value, created_at) " +
                             "values (10000, 10000, '" + HOST + "', 'abc/123', '2010-10-10')");

        userSettingsService.saveSetting(10000L, HOST, "XXX");

        String actualValue = jdbcTemplate
                .queryForObject("select value from user_settings where id = 10000", String.class);

        assertThat(actualValue).isEqualTo("XXX");
    }

    @Test
    void shouldSaveNewSetting() {
        userSettingsService.saveSetting(10000L, HOST, "XXX");

        String actualValue = jdbcTemplate
                .queryForObject("select value from user_settings where player_id = 10000 and key = '" + HOST + "'", String.class);

        assertThat(actualValue).isEqualTo("XXX");
    }
}
