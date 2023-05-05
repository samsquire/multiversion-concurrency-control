package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SoftLock extends Thread {
    private final ReentrantReadWriteLock.WriteLock lock;
    private int id;
    private List<SoftLock> threads;
    private boolean running = true;

    private List<Lock> waiting = new ArrayList<>();
    private List<Lock> incoming = new ArrayList<>();
    private long n;

    public SoftLock(int id) {
        this.id = id;
        this.lock = new ReentrantReadWriteLock().writeLock();
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        long seconds = 5;
        List<SoftLock> threads = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            SoftLock thread = new SoftLock(x);
            threads.add(thread);
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).setThreads(new ArrayList<>(threads));
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(seconds * 1000);
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).running = false;
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).join();
        }
        long end = System.currentTimeMillis();

        long total = 0;
        for (int x = 0; x < threadCount; x++) {
            total += threads.get(x).n;
        }
        long duration = end - start;
        System.out.println(String.format("Requests %d", total));
        System.out.println(String.format("Duration %d", duration / 1000));
        System.out.println(String.format("Requests per second %d", total / seconds));


    }

    private void setThreads(ArrayList<SoftLock> threads) {
        this.threads = threads;
    }

    public void run() {
        Random rng = new Random();
        List<Lock> removals = new ArrayList<>(1000);
        while (running) {
                for (int b = 0; b < 1000; b++) {
                    int pickedLock = rng.nextInt(threads.size());
                    SoftLock target = threads.get(pickedLock);
                    Lock currentLock = new Lock(pickedLock, this, target);
                    target.submit(currentLock);
                    waiting.add(currentLock);
                }

            for (int x = 0; x < waiting.size(); x++) {
                Lock lock1 = waiting.get(x);
                if (lock1.finished) {
                    n++;
                    removals.add(lock1);
                }
            }
            waiting.removeAll(removals);
            removals.clear();

            lock.lock();
            for (Lock lockWaiting : incoming) {
                lockWaiting.finish();
            }
            incoming.clear();
            lock.unlock();

        }
    }

    private void submit(Lock currentLock) {
        currentLock.target.lock.lock();
        currentLock.target.incoming.add(currentLock);
        currentLock.target.lock.unlock();
    }

    private class Lock {
        public volatile boolean finished;
        public volatile boolean handled;
        private int pickedLock;
        private SoftLock owner;
        private SoftLock target;

        public Lock(int pickedLock, SoftLock owner, SoftLock target) {
            this.pickedLock = pickedLock;
            this.owner = owner;
            this.target = target;
        }

        public void finish() {
            finished = true;

        }
    }
}
