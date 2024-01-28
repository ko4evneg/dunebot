package ru.trainithard.dunebot.service;

import java.util.Set;

public class TelegramMessageBuilder implements MessageBuilder {
    private final StringBuilder stringBuilder = new StringBuilder();

    @Override
    public void append(String text) {
        String escapedText = MarkdownEscaper.getEscapedText(text);
        stringBuilder.append(escapedText);
    }

    @Override
    public void appendBold(String text) {
        String escapedText = MarkdownEscaper.getEscapedText(text);
        stringBuilder.append("*").append(escapedText).append("*");
    }

    @Override
    public String build() {
        return stringBuilder.toString();
    }

    private static class MarkdownEscaper {
        private static final Set<Character> escapedChars = Set.of('_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!');

        private static String getEscapedText(String text) {
            StringBuilder escapeTextBuilder = new StringBuilder();
            for (char ch : text.toCharArray()) {
                if (escapedChars.contains(ch)) {
                    escapeTextBuilder.append('\\').append(ch);
                } else {
                    escapeTextBuilder.append(ch);
                }
            }
            return escapeTextBuilder.toString();
        }
    }
}
