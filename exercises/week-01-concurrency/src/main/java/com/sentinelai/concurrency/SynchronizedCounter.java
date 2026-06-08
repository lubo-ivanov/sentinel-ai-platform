package com.sentinelai.concurrency;

public class SynchronizedCounter implements Counter{
    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }

}
