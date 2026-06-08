package com.sentinelai.concurrency;

import java.util.ArrayList;
import java.util.List;

public class CounterRaceDemo {

    private static final int THREAD_COUNT = 100;
    private static final int INCREMENTS_PER_THREAD = 1000;
    private static final int EXPECTED_COUNT = THREAD_COUNT * INCREMENTS_PER_THREAD;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Expected count: " + EXPECTED_COUNT);

        for (Counter counter : new Counter[]{new UnsafeCounter(), new SynchronizedCounter(), new AtomicCounter()}) {
            runCounter(counter);
            System.out.println(counter.getClass().getSimpleName() + " result: " + counter.getCount());
        }
    }

    private static void runCounter(Counter counter) throws InterruptedException {
        List<Thread> threads = createThreads(() -> {
            for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                counter.increment();
            }
        });

        startAndJoin(threads);
    }

    private static List<Thread> createThreads(Runnable task) {
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads.add(new Thread(task));
        }

        return threads;
    }

    private static void startAndJoin(List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}