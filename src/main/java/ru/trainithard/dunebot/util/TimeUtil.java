package ru.trainithard.dunebot.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class TimeUtil {
    private TimeUtil() {
    }

    public static Instant getRounded(Instant instant, int hours, int minutes) {
        return instant.atZone(ZoneId.systemDefault())
                .withHour(hours)
                .withMinute(minutes)
                .truncatedTo(ChronoUnit.MINUTES)
                .toInstant();
    }
}
