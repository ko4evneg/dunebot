package ru.trainithard.dunebot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enum describing available game modes.
 */
@Getter
@RequiredArgsConstructor
public enum ModType {
    CLASSIC("\"Дюна (классика)\"", "dune", 4),
    UPRISING_4("\"Апрайзинг на 4-х\"", "up4", 4),
    UPRISING_6("\"Апрайзинг 3х3\"", "up6", 6);

    private static final Map<String, ModType> modTypeByAlias;

    /**
     * Title to display in polls.
     */
    private final String modName;
    /**
     * Name for command usage.
     */
    private final String alias;
    /**
     * Maximum allowed players count.
     */
    private final int playersCount;

    static {
        modTypeByAlias = Arrays.stream(ModType.values())
                .collect(Collectors.toMap(ModType::getAlias, Function.identity()));
    }

    public static ModType getByAlias(String alias) {
        return modTypeByAlias.get(alias.toLowerCase());
    }
}
