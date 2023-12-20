package ru.trainithard.dunebot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ModType {
    CLASSIC("\"Дюна (классика)\"", 4),
    UPRISING_4("\"Апрайзинг на 4-х\"", 4),
    UPRISING_6("\"Апрайзинг 3х3\"", 6);

    private final String modName;
    private final int playersCount;
}
