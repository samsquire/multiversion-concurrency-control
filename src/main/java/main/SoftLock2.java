package main;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SoftLock2 extends Thread {
    private final ReentrantReadWriteLock.WriteLock lock;
    private int id;
    private int locksPerThread;
    private List<SoftLock2> threads;
    private boolean running = true;

    private List<Lock> waiting = new ArrayList<>();
    private ArrayList<Lock>[] incoming;
    private long n;
    private int nextFree;

    public SoftLock2(int id, int locksPerThread) {
        this.id = id;
        this.locksPerThread = locksPerThread;
        this.lock = new ReentrantReadWriteLock().writeLock();
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        long seconds = 5;
        int locksSize = 200000;
        int locksPerThread = locksSize / threadCount;
        List<SoftLock2> threads = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            SoftLock2 thread = new SoftLock2(x, locksPerThread);
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

    private void setThreads(ArrayList<SoftLock2> threads) {
        this.threads = threads;
        incoming = new ArrayList[threads.size()];
        for (int x = 0; x < threads.size(); x++) {
            incoming[x] = new ArrayList<>();
        }
    }

    public void run() {
        Random rng = new Random();
        List<Lock> removals = new ArrayList<>(1000);
        Map<Integer, List<Lock>> locks = new HashMap<>();

        while (running) {
            if (waiting.size() == 0) {
                for (int x = 0; x < threads.size(); x++) {
                    locks.put(x, new ArrayList<>());
                }
            for (int x = 0 ; x < locksPerThread; x++) {
                int pickedLock = rng.nextInt(threads.size());
                SoftLock2 target = threads.get(pickedLock);
                Lock currentLock = new Lock(pickedLock, this, target);

                locks.get(pickedLock).add(currentLock);

                waiting.add(currentLock);
            }
                for (int x = 0 ; x < threads.size() ; x++) {
                    threads.get(id).lock.lock();
                    threads.get(x).incoming[id].addAll(locks.get(x));
                    threads.get(id).lock.unlock();

                }
            }

            for (int x = 0; x < waiting.size(); x++) {
                Lock lock1 = waiting.get(x);
                if (!lock1.handled && lock1.finished) {
                    n++;
                    lock1.handled = true;
                    removals.add(lock1);
                }
            }
            waiting.removeAll(removals);
            removals.clear();
            for (int x = 0 ; x < threads.size() ; x++) {
                    List<Lock> remove = incoming[x];
                    threads.get(x).lock.lock();
                    for (int y = 0; y < remove.size(); y++) {
                        Lock item = remove.get(y);
                        item.finish();
                    }
                    threads.get(x).lock.unlock();
            }

        }
    }


    private class Lock {
        public volatile boolean finished;
        public volatile boolean handled;
        private int pickedLock;
        private SoftLock2 owner;
        private SoftLock2 target;
        private ReentrantReadWriteLock.WriteLock lock;
        private boolean free;

        public Lock(int pickedLock, SoftLock2 owner, SoftLock2 target) {
            this.pickedLock = pickedLock;
            this.owner = owner;
            this.target = target;
            this.lock = new ReentrantReadWriteLock().writeLock();
        }

        public void finish() {
            finished = true;
        }

    }
}
