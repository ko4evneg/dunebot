package ru.trainithard.dunebot.service.telegram;

import org.junit.jupiter.api.RepeatedTest;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramBotTest {
    private final TelegramBot telegramBot = new TelegramBot("uName", "token");

    @RepeatedTest(10)
    void shouldCorrectlyProcessMultithreadedPushPolls() {
        AtomicBoolean supplyingFinished = new AtomicBoolean(false);

        Runnable updateSupplier = () -> {
            for (int i = 0; i < 100_000; i++) {
                Update update = new Update();
                update.setUpdateId(i);
                telegramBot.onUpdateReceived(update);
            }
            supplyingFinished.set(true);
        };

        Set<Integer> updatesConsumed = ConcurrentHashMap.newKeySet();
        Runnable updateConsumer = () -> {
            Update update = telegramBot.poll();
            while (!supplyingFinished.get() || update != null) {
                if (update != null) {
                    Integer updateId = update.getUpdateId();
                    assertThat(updatesConsumed).doesNotContain(updateId);
                    updatesConsumed.add(updateId);
                } else {
                    try {
                        TimeUnit.MICROSECONDS.sleep(3);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                update = telegramBot.poll();
            }
        };

        Thread supplier = new Thread(updateSupplier);
        Thread consumer1 = new Thread(updateConsumer);
        Thread consumer2 = new Thread(updateConsumer);
        Thread consumer3 = new Thread(updateConsumer);

        supplier.start();
        consumer1.start();
        consumer2.start();
        consumer3.start();

        try {
            supplier.join();
            consumer1.join();
            consumer2.join();
            consumer3.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertThat(updatesConsumed).hasSize(100_000);
    }
}
