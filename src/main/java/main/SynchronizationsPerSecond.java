package main;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SynchronizationsPerSecond extends Thread {
    private final ReentrantReadWriteLock.WriteLock lock;
    private boolean running;
    private long n;

    public SynchronizationsPerSecond(ReentrantReadWriteLock.WriteLock writeLock) {
        this.lock = writeLock;
    }

    public static void  main(String argv[]) throws InterruptedException {
        long seconds = 5;
        ArrayList<SynchronizationsPerSecond> threads = new ArrayList<>();
        for (int i = 0 ; i < 2 ; i++) {
            ReentrantReadWriteLock.WriteLock writeLock = new ReentrantReadWriteLock().writeLock();
            SynchronizationsPerSecond thread = new SynchronizationsPerSecond(writeLock);
            threads.add(thread);
        }
        for (SynchronizationsPerSecond thread : threads) {
            thread.running = true;
        }
        for (SynchronizationsPerSecond thread : threads) {
            thread.start();
        }
        Thread.sleep(seconds * 1000);
        for (SynchronizationsPerSecond thread : threads) {
            thread.running = false;
        }
        for (SynchronizationsPerSecond thread : threads) {
            thread.join();
        }
        long total = 0;
        for (SynchronizationsPerSecond thread : threads) {
            total += thread.n;
        }
        System.out.println(String.format("Synchronizations per second %d", total / seconds));
    }
    public void run() {
        while (running) {
            lock.lock();
            n++;
            lock.unlock();
        }
    }
}
