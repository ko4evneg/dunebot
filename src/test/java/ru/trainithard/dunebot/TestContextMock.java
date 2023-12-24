package ru.trainithard.dunebot;

import org.springframework.boot.test.mock.mockito.MockBean;
import ru.trainithard.dunebot.service.telegram.TelegramBot;

public abstract class TestContextMock {
    @MockBean
    protected TelegramBot telegramBot;
}
