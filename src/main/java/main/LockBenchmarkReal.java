package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockBenchmarkReal extends Thread {
    private final HashMap<LockBenchmarkReal, Long> map;
    private LockBenchmarkReal main;

    private final int id;
    private volatile boolean buffer;
    private final Lock writeLock;
    private volatile boolean running = true;
    private DoublyLinkedList data;
    private String mode;
    private List<LockBenchmarkReal> threads;
    private long counter;


    public LockBenchmarkReal(String mode, int id, Lock writeLock) {
        this.main = main;
        this.id = id;
        this.writeLock = writeLock;
        this.mode = mode;
        this.map = new HashMap<LockBenchmarkReal, Long>();
        if (main == null) {
            this.data = new DoublyLinkedList(0, System.currentTimeMillis());
            this.main = this;
        }

    }

    public void run() {

        if (mode.equals("counter")) {
            while (running) {
                writeLock.lock();
                counter++;
                writeLock.unlock();
            }
        } else if (mode.equals("nolock")) {
            while (running) {
                counter += 1;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        List<LockBenchmarkReal> threads = new ArrayList<>();
        ReadWriteLock rwlock = new ReentrantReadWriteLock();

        for (int i = 1 ; i < threadCount; i++) {

            LockBenchmarkReal lockBenchmark = new LockBenchmarkReal("counter", i, rwlock.writeLock());
            threads.add(lockBenchmark);
            lockBenchmark.setThreads(threads);
        }


        long start = System.currentTimeMillis();
        for (LockBenchmarkReal loopBenchmark : threads) {
            loopBenchmark.start();
        }
        Thread.sleep(10000);
        for (LockBenchmarkReal loopBenchmark : threads) {
            loopBenchmark.running = false;
        }
        for (LockBenchmarkReal loopBenchmark : threads) {
            loopBenchmark.join();
        }
        Thread.sleep(100);
        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        long totalRequests = 0;
        for (LockBenchmarkReal thread : threads) {
            totalRequests += thread.counter;
            System.out.println(String.format("Thread counter %d", thread.counter));
        }

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }

    private void setThreads(List<LockBenchmarkReal> threads) {
        this.threads = threads;
    }
}
