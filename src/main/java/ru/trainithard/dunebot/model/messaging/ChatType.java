package ru.trainithard.dunebot.model.messaging;

public enum ChatType {
    PRIVATE, GROUP, SUPERGROUP, CHANNEL;

    public String getValue() {
        return this.name().toLowerCase();
    }
}
