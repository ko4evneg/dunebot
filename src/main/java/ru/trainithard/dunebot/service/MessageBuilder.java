package ru.trainithard.dunebot.service;

public interface MessageBuilder {
    void append(String text);

    void appendBold(String text);

    String build();
}
