package ru.trainithard.dunebot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum ModType {
    CLASSIC("\"Дюна (классика)\"", "dune", 4),
    UPRISING_4("\"Апрайзинг на 4-х\"", "up4", 4),
    UPRISING_6("\"Апрайзинг 3х3\"", "up6", 6);

    private static final Map<String, ModType> modTypeByAlias;

    private final String modName;
    private final String alias;
    private final int playersCount;

    static {
        modTypeByAlias = Arrays.stream(ModType.values())
                .collect(Collectors.toMap(ModType::getAlias, Function.identity()));
    }

    public static ModType getByAlias(String alias) {
        return modTypeByAlias.get(alias.toLowerCase());
    }
}
