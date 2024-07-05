package ru.trainithard.dunebot.service;

import org.springframework.stereotype.Component;
import ru.trainithard.dunebot.model.Match;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class LogId {
    private static final Random random = new Random();
    private static final Map<Long, Integer> loggingIdByThreadId = new ConcurrentHashMap<>();

    private LogId() {
    }

    public static void init() {
        loggingIdByThreadId.put(getThreadId(), random.nextInt(0, 1_000_000));
    }

    public static int get() {
        return Objects.requireNonNullElse(loggingIdByThreadId.get(getThreadId()), -1);
    }

    public static void clear() {
        loggingIdByThreadId.remove(getThreadId());
    }

    private static long getThreadId() {
        return Thread.currentThread().getId();
    }

    //TODO remove after debug:
    public static String getMatchLogInfo(Match match) {
        String playerPlaces = match.getMatchPlayers().stream()
                .map(matchPlayer ->
                        String.format("player %d, candidate: %d", matchPlayer.getPlayer().getId(), matchPlayer.getPlace()))
                .collect(Collectors.joining("; "));
        StringBuilder stringBuilder = new StringBuilder(playerPlaces).append("; ");
        stringBuilder.append("state: ").append(match.getState()).append(", votes: ").append(match.getPositiveAnswersCount());
        return stringBuilder.toString();
    }
}
