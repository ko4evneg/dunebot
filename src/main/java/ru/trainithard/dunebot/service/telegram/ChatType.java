package ru.trainithard.dunebot.service.telegram;

public enum ChatType {
    PRIVATE, GROUP, SUPERGROUP, CHANNEL;

    public String getValue() {
        return this.name().toLowerCase();
    }
}
