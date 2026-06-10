package com.sentinelai.sentinel;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IncidentStoreConcurrencyTest {
    @Test
    void concurrentCreatesAllSucceed() throws InterruptedException {
        IncidentStore store = new IncidentStore();
        int threadCount = 100;
        int initialSize = store.findAll().size();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    store.create(new IncidentRequest("incident-" + id, "minor"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(initialSize + threadCount, store.findAll().size());
    }
}
