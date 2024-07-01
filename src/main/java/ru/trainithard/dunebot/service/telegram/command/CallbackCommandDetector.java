package ru.trainithard.dunebot.service.telegram.command;

import java.util.Map;

public class CallbackCommandDetector {
    public static final String LEADER_CALLBACK_SYMBOL = "_L_";
    public static final String SUBMIT_PLAYERS_CALLBACK_SYMBOL = "_SP_";
    private static final Map<String, Command> commandByCallbackText = Map.of(
            LEADER_CALLBACK_SYMBOL, Command.LEADER,
            SUBMIT_PLAYERS_CALLBACK_SYMBOL, Command.PLAYER_ACCEPT
    );

    private CallbackCommandDetector() {
    }

    public static Command getCommandBy(String callback) {
        return commandByCallbackText.entrySet().stream()
                .filter(stringCommandEntry -> callback.contains(stringCommandEntry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }
}
