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

    public static ModType getByAlias(String alias) {
        return switch (alias.toLowerCase()) {
            case "dune" -> ModType.CLASSIC;
            case "up4" -> ModType.UPRISING_4;
            case "up6" -> ModType.UPRISING_6;
            default -> null;
        };
    }
}
