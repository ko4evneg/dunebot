package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.trainithard.dunebot.TestContextMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SettingsServiceImplTest extends TestContextMock {
    @Autowired
    private SettingsService settingsService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10000, 'strKey', 'strVal', '2010-01-02')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10001, 'intKey', '5', '2010-01-02')");
        jdbcTemplate.execute("insert into settings (id, key, value, created_at) values (10002, 'longKey', '2', '2010-01-02')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from settings where id between 10000 and 10002 or key = 'randomKey'");
        System.out.println();
    }

    @Test
    void shouldReturnStringSetting() {
        String actualValue = settingsService.getStringSetting("strKey");

        assertEquals("strVal", actualValue);
    }

    @Test
    void shouldReturnCachedStringSetting() {
        settingsService.getStringSetting("strKey");
        jdbcTemplate.execute("delete from settings where key = 'strKey'");

        String actualCachedValue = settingsService.getStringSetting("strKey");

        assertEquals("strVal", actualCachedValue);
    }

    @Test
    void shouldReturnLongSetting() {
        long actualValue = settingsService.getLongSetting("longKey");

        assertEquals(2L, actualValue);
    }

    @Test
    void shouldReturnCachedLongSetting() {
        settingsService.getLongSetting("longKey");
        jdbcTemplate.execute("delete from settings where key = 'longKey'");

        long actualCachedValue = settingsService.getLongSetting("longKey");

        assertEquals(2L, actualCachedValue);
    }

    @Test
    void shouldReturnIntSetting() {
        int actualValue = settingsService.getIntSetting("intKey");

        assertEquals(5, actualValue);
    }

    @Test
    void shouldReturnCachedIntSetting() {
        settingsService.getIntSetting("intKey");
        jdbcTemplate.execute("delete from settings where key = 'intKey'");

        int actualCachedValue = settingsService.getIntSetting("intKey");

        assertEquals(5, actualCachedValue);
    }

    @Test
    void shouldSaveSetting() {
        settingsService.saveSetting("randomKey", "val");

        String actualValue = jdbcTemplate.queryForObject("select value from settings where key = 'randomKey'", String.class);

        assertEquals("val", actualValue);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void shouldUpdateSetting() {
        settingsService.saveSetting("intKey", "100");

        Integer actualValue = jdbcTemplate.queryForObject("select value from settings where key = 'intKey'", Integer.class);

        assertNotNull(actualValue);
        assertEquals(100, actualValue);
    }
}
