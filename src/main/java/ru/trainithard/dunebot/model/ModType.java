package ru.trainithard.dunebot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.trainithard.dunebot.service.telegram.command.Command;

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
    CLASSIC("'Дюна (классика)'", Command.NEW_DUNE, "dune", 4),
    BUFF("'Дюна (БАФ)'", Command.NEW_BUFF, "dune", 4),
    UPRISING_4("'Апрайзинг на 4-х'", Command.NEW_UP4, "up4", 4),
    UPRISING_6("'Апрайзинг 3х3''", Command.NEW_UP6, "up6", 6);

    private static final Map<Command, ModType> modTypeByAlias;

    /**
     * Title to display in polls.
     */
    private final String modName;
    /**
     * Creation command.
     */
    private final Command command;
    /**
     * Alias for ModType
     */
    private final String alias;
    /**
     * Maximum allowed players count.
     */
    private final int playersCount;

    static {
        modTypeByAlias = Arrays.stream(ModType.values())
                .collect(Collectors.toMap(ModType::getCommand, Function.identity()));
    }

    public static ModType getByCommand(Command command) {
        ModType modType = modTypeByAlias.get(command);
        if (modType == null) {
            throw new IllegalStateException("Неверный тип матча");
        }
        return modType;
    }
}
