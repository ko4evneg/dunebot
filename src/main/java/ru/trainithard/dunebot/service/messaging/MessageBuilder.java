package ru.trainithard.dunebot.service.messaging;

import ru.trainithard.dunebot.configuration.SettingConstants;

public class MessageBuilder {
    private final StringBuilder stringBuilder = new StringBuilder();

    public MessageBuilder append(String text) {
        stringBuilder.append(text);
        return this;
    }

    public MessageBuilder appendBold(String text) {
        stringBuilder.append("*").append(text).append("*");
        return this;
    }

    public MessageBuilder appendLink(String name, String link) {
        //TODO: escape in (...)
        stringBuilder.append("[*]").append(name).append("]").append("(").append(link).append(")");
        return this;
    }

    public MessageBuilder newLine() {
        stringBuilder.append(SettingConstants.EXTERNAL_LINE_SEPARATOR);
        return this;
    }

    public String getText() {
        return stringBuilder.toString();
    }
}
