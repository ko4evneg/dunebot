package ru.trainithard.dunebot.service.messaging;

import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.util.MarkdownEscaper;

public class ExternalMessage {
    private final StringBuilder stringBuilder = new StringBuilder();

    public ExternalMessage() {
    }

    public ExternalMessage(String text) {
        append(text);
    }

    public ExternalMessage append(String text) {
        stringBuilder.append(MarkdownEscaper.getEscaped(text));
        return this;
    }

    public ExternalMessage appendBold(String text) {
        stringBuilder.append("*").append(MarkdownEscaper.getEscaped(text)).append("*");
        return this;
    }

    public ExternalMessage appendLink(String name, String link) {
        //TODO: escape in (...)
        stringBuilder.append("[").append(name).append("]").append("(").append(link).append(")");
        return this;
    }

    public ExternalMessage newLine() {
        stringBuilder.append(SettingConstants.EXTERNAL_LINE_SEPARATOR);
        return this;
    }

    public String getText() {
        return stringBuilder.toString();
    }
}
