package ru.trainithard.dunebot.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class LogIdTest {
    @Test
    void shouldGetCorrectLogIdForThread() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Callable<Boolean> callable1 = () -> {
            LogId.init();
            int logId1 = MockTestIdConsumer1.getLogId();
            int logId2 = MockTestIdConsumer2.getLogId();
            return logId1 == logId2;
        };
        Callable<Boolean> callable2 = () -> {
            LogId.init();
            int logId1 = MockTestIdConsumer1.getLogId();
            int logId2 = MockTestIdConsumer2.getLogId();
            return logId1 == logId2;
        };
        Callable<Boolean> callable3 = () -> {
            LogId.init();
            int logId1 = MockTestIdConsumer1.getLogId();
            int logId2 = MockTestIdConsumer2.getLogId();
            return logId1 == logId2;
        };

        List<Future<Boolean>> futureList = executorService.invokeAll(List.of(callable1, callable2, callable3));
        executorService.awaitTermination(200, TimeUnit.MILLISECONDS);
        boolean hasMismatchingId = futureList.stream()
                .map(booleanFuture -> {
                    try {
                        return booleanFuture.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .anyMatch(bool -> !bool);

        assertThat(hasMismatchingId).isFalse();
    }

    private static class MockTestIdConsumer1 {
        public static int getLogId() {
            return LogId.get();
        }
    }

    private static class MockTestIdConsumer2 {
        public static int getLogId() {
            return LogId.get();
        }
    }
}
