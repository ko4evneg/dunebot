package ru.trainithard.dunebot.service.telegram.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum CallbackSymbol {
    RESUBMIT_CALLBACK_SYMBOL("_RES_", Command.RESUBMIT),
    SUBMIT_PLAYERS_CALLBACK_SYMBOL("_SP_", Command.PLAYER_ACCEPT),
    SUBMIT_LEADERS_CALLBACK_SYMBOL("_SL_", Command.LEADER_ACCEPT);

    private static final Map<String, Command> commandByCallbackText = Arrays.stream(CallbackSymbol.values())
            .collect(Collectors.toMap(callbackSymbol -> callbackSymbol.symbol, callbackSymbol -> callbackSymbol.command));
    @Getter
    private final String symbol;
    private final Command command;

    public static Command getCommandBy(String callback) {
        return commandByCallbackText.entrySet().stream()
                .filter(stringCommandEntry -> callback.contains(stringCommandEntry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }
}
