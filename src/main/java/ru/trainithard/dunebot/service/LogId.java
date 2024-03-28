package ru.trainithard.dunebot.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LogId {
    private static final Random random = new Random();
    private static final Map<Long, Integer> loggingIdByThreadId = new ConcurrentHashMap<>();

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
}
