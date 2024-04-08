package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.SettingKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SettingsServiceImplTest extends TestContextMock {
    @Autowired
    private SettingsService settingsService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10000, 'CHAT_ID', 'strVal', '2010-01-02')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10001, 'TOPIC_ID_CLASSIC', '5', '2010-01-02')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10002, 'ADMIN_USER_ID', '2', '2010-01-02')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from settings where id between 10000 and 10002 or key = '" + SettingKey.CHAT_ID + "'");
    }

    @Test
    void shouldReturnStringSetting() {
        String actualValue = settingsService.getStringSetting(SettingKey.CHAT_ID);

        assertEquals("strVal", actualValue);
    }

    @Test
    void shouldReturnLongSetting() {
        long actualValue = settingsService.getLongSetting(SettingKey.ADMIN_USER_ID);

        assertEquals(2L, actualValue);
    }

    @Test
    void shouldReturnIntSetting() {
        int actualValue = settingsService.getIntSetting(SettingKey.TOPIC_ID_CLASSIC);

        assertEquals(5, actualValue);
    }

    @Test
    void shouldSaveSetting() {
        jdbcTemplate.execute("delete from settings where id = 10000");
        settingsService.saveSetting(SettingKey.CHAT_ID, "val");

        String actualValue = jdbcTemplate.queryForObject("select value from settings where key = '" + SettingKey.CHAT_ID + "'", String.class);

        assertEquals("val", actualValue);
    }

    @Test
    void shouldUpdateSetting() {
        settingsService.saveSetting(SettingKey.TOPIC_ID_CLASSIC, "100");

        Integer actualValue = jdbcTemplate.queryForObject("select value from settings where key = '" + SettingKey.TOPIC_ID_CLASSIC + "'", Integer.class);

        assertNotNull(actualValue);
        assertEquals(100, actualValue);
    }
}
