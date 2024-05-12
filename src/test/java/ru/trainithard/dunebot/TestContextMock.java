package ru.trainithard.dunebot;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

import java.util.Objects;

public abstract class TestContextMock {
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected CacheManager cacheManager;
    @MockBean
    protected TelegramBot telegramBot;
    @MockBean
    protected MessagingService messagingService;

    @AfterEach
    void superAfterEach() {
        Objects.requireNonNull(cacheManager.getCache("settings")).invalidate();
    }
}
