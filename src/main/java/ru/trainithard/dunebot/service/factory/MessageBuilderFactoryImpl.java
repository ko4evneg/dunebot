package ru.trainithard.dunebot.service.factory;

import org.springframework.stereotype.Service;
import ru.trainithard.dunebot.service.MessageBuilder;
import ru.trainithard.dunebot.service.TelegramMessageBuilder;

@Service
public class MessageBuilderFactoryImpl implements MessageBuilderFactory {
    @Override
    public MessageBuilder getInstance() {
        return new TelegramMessageBuilder();
    }
}
