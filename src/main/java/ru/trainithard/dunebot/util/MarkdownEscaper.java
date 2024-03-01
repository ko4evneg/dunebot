package ru.trainithard.dunebot.util;

import java.util.Set;

public class MarkdownEscaper {
    private static final Set<Character> escapedChars =
            Set.of('_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!');

    public static String getEscaped(String text) {
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
