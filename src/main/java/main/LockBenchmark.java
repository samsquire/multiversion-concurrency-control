package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockBenchmark extends Thread {
    private final HashMap<LockBenchmark, Long> map;
    private LockBenchmark main;

    private final int id;
    private volatile boolean buffer;
    private final Lock writeLock;
    private volatile boolean running = true;
    private long left = 0;
    private long right = 0;
    private DoublyLinkedList data;
    private String mode;
    private List<LockBenchmark> threads;
    private long counter;
    private int lefts;
    private int rights;

    public LockBenchmark(String mode, int id, Lock writeLock) {
        this.main = main;
        this.id = id;
        this.writeLock = writeLock;
        this.mode = mode;
        this.map = new HashMap<LockBenchmark, Long>();
        if (main == null) {
            this.data = new DoublyLinkedList(0, System.currentTimeMillis());
            this.main = this;
        }

    }

    public void run() {
        if (mode.equals("reader")) {
            while (running) {
                long total = 0;
                for (LockBenchmark thread : threads) {
                    if (thread.buffer) {
                        long left1 = thread.left;
                        total += left1;
                        thread.right = thread.left;
                        map.put(thread, left1);
                        thread.buffer = !thread.buffer;
                    } else {
                        long right1 = thread.right;
                        total += right1;
                        thread.left = right1;
                        map.put(thread, right1);
                        thread.buffer = !thread.buffer;

                    }
                }
                counter = total;
            }
        }
        if (mode.equals("counter")) {
            while (running) {
                if (buffer) {
                    left++;
                } else {
                    right++;
                }

            }
        } else if (mode.equals("nolock")) {
            while (running) {
                counter += 1;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        List<LockBenchmark> threads = new ArrayList<>();

        for (int i = 1 ; i < threadCount; i++) {
            ReadWriteLock rwlock = new ReentrantReadWriteLock();

            LockBenchmark lockBenchmark = new LockBenchmark("counter", i, rwlock.writeLock());
            threads.add(lockBenchmark);
        }
        ReadWriteLock rwlock = new ReentrantReadWriteLock();

        LockBenchmark reader = new LockBenchmark("reader", 0, rwlock.writeLock());
        reader.setThreads(threads);

        reader.start();
        long start = System.currentTimeMillis();
        for (LockBenchmark loopBenchmark : threads) {
            loopBenchmark.start();
        }
        Thread.sleep(10000);
        for (LockBenchmark loopBenchmark : threads) {
            loopBenchmark.running = false;
        }
        reader.running = false;
        for (LockBenchmark loopBenchmark : threads) {
            loopBenchmark.join();
        }
        Thread.sleep(100);
        reader.join();
        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        long totalRequests = reader.counter;

        for (LockBenchmark thread : threads) {
            assert thread.left == reader.map.get(thread) || thread.right == reader.map.get(thread);
        }

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }

    private void setThreads(List<LockBenchmark> threads) {
        this.threads = threads;
    }
}
