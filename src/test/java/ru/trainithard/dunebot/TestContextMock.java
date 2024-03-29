package ru.trainithard.dunebot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.trainithard.dunebot.service.messaging.MessagingService;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

public abstract class TestContextMock {
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @MockBean
    protected TelegramBot telegramBot;
    @MockBean
    protected MessagingService messagingService;
}
