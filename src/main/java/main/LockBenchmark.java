package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockBenchmark extends Thread {
    private final int id;
    private final Lock writeLock;
    private boolean running = true;
    private Integer counter = 0;

    public LockBenchmark(int id, Lock writeLock) {
        this.id = id;
        this.writeLock = writeLock;

    }

    public void run() {
        while (running) {
            writeLock.lock();
            counter++;
            writeLock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReadWriteLock rwlock = new ReentrantReadWriteLock();
        int threadCount = 11;
        List<LockBenchmark> threads = new ArrayList<>();
        for (int i = 0 ; i < threadCount; i++) {
            LockBenchmark lockBenchmark = new LockBenchmark(i, rwlock.writeLock());
            threads.add(lockBenchmark);
        }
        long start = System.currentTimeMillis();
        for (LockBenchmark loopBenchmark : threads) {
            loopBenchmark.start();
        }
        Thread.sleep(5000);
        for (LockBenchmark loopBenchmark : threads) {
            loopBenchmark.running = false;
        }
        for (LockBenchmark loopBenchmark : threads) {
            loopBenchmark.join();
        }
        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        int totalRequests = threads.get(0).counter;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }
}
