package ru.trainithard.dunebot.model;

import lombok.Getter;

@Getter
public enum ModType {
    CLASSIC("\"Дюна (классика)\""), UPRISING_4("\"Апрайзинг на 4-х\""), UPRISING_6("\"Апрайзинг 3х3\"");

    private final String modName;

    ModType(String modName) {
        this.modName = modName;
    }
}
