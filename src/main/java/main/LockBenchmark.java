package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockBenchmark extends Thread {
    private LockBenchmark main;
    private final int id;
    private final Lock writeLock;
    private boolean running = true;
    private Integer counter = 0;
    private DoublyLinkedList data;
    private String mode;

    public LockBenchmark(String mode, LockBenchmark main, int id, Lock writeLock) {
        this.main = main;
        this.id = id;
        this.writeLock = writeLock;
        this.mode = mode;
        if (main == null) {
            this.data = new DoublyLinkedList(0, System.currentTimeMillis());
            this.main = this;
        }

    }

    public void run() {
        if (mode.equals("counter")) {
            while (running) {
                writeLock.lock();
                counter += 10;
                writeLock.unlock();
            }
        } else if (mode.equals("linkedlist")) {
            while (running) {
                writeLock.lock();
                int nextValue = 0;
                if (main.data.tail == null) {
                    nextValue = Integer.MIN_VALUE;
                } else {
                    nextValue = main.data.tail.value;
                }
                main.counter++;
                main.data.insert(nextValue + 1);
                writeLock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ReadWriteLock rwlock = new ReentrantReadWriteLock();
        int threadCount = 11;
        List<LockBenchmark> threads = new ArrayList<>();
        LockBenchmark main = new LockBenchmark("counter", null, 0, rwlock.writeLock());
        threads.add(main);
        for (int i = 1 ; i < threadCount; i++) {
            LockBenchmark lockBenchmark = new LockBenchmark("counter", main, i, rwlock.writeLock());
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
