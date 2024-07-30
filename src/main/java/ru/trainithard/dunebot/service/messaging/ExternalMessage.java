package ru.trainithard.dunebot.service.messaging;

import ru.trainithard.dunebot.configuration.SettingConstants;
import ru.trainithard.dunebot.util.MarkdownEscaper;

import static ru.trainithard.dunebot.configuration.SettingConstants.EXTERNAL_LINE_SEPARATOR;

public class ExternalMessage {
    private final StringBuilder stringBuilder = new StringBuilder();
    private boolean boldMarkupEnabled;

    public ExternalMessage() {
    }

    public ExternalMessage(Object text) {
        append(text.toString());
    }

    public ExternalMessage appendRaw(Object text) {
        stringBuilder.append(text.toString());
        return this;
    }

    public ExternalMessage append(Object text) {
        stringBuilder.append(MarkdownEscaper.getEscaped(text.toString()));
        return this;
    }

    public ExternalMessage appendBold(Object text) {
        stringBuilder.append("*").append(MarkdownEscaper.getEscaped(text.toString())).append("*");
        return this;
    }

    public ExternalMessage appendInline(Object text) {
        stringBuilder.append("`").append(MarkdownEscaper.getEscaped(text.toString())).append("`");
        return this;
    }

    public ExternalMessage startBold() {
        stringBuilder.append("*");
        if (boldMarkupEnabled) {
            throw new IllegalStateException("Starting new bold markup, when it is already bold");
        }
        boldMarkupEnabled = true;
        return this;
    }

    public ExternalMessage endBold() {
        stringBuilder.append("*");
        if (!boldMarkupEnabled) {
            throw new IllegalStateException("No bold markup start registered");
        }
        boldMarkupEnabled = false;
        return this;
    }

    public ExternalMessage appendLink(Object name, String link) {
        //TODO: escape in (...)
        stringBuilder.append("[").append(name).append("]").append("(").append(link).append(")");
        return this;
    }

    public ExternalMessage appendBoldLink(Object name, String link) {
        //TODO: escape in (...)
        stringBuilder.append("[*").append(name).append("*]").append("(").append(link).append(")");
        return this;
    }

    public ExternalMessage newLine() {
        stringBuilder.append(SettingConstants.EXTERNAL_LINE_SEPARATOR);
        return this;
    }

    public ExternalMessage space() {
        stringBuilder.append(" ");
        return this;
    }

    public ExternalMessage trimTrailingNewLine() {
        int lastCharIndex = stringBuilder.length() - 1;
        char lastChar = stringBuilder.charAt(lastCharIndex);
        if (EXTERNAL_LINE_SEPARATOR.equals(String.valueOf(lastChar))) {
            stringBuilder.deleteCharAt(lastCharIndex);
        }
        return this;
    }

    public String getText() {
        int index = stringBuilder.lastIndexOf(EXTERNAL_LINE_SEPARATOR);
        if (stringBuilder.length() > 0 && index == stringBuilder.length() - 1) {
            stringBuilder.deleteCharAt(index);
        }
        return stringBuilder.toString();
    }

    public ExternalMessage concat(ExternalMessage otherMessage) {
        stringBuilder.append(otherMessage.getText());
        return this;
    }
    //TODO ADD TEST
}
