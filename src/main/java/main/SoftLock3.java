package main;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SoftLock3 extends Thread {
    private final ReentrantReadWriteLock.WriteLock lock;
    private int id;
    private int locksPerThread;
    private int sublocks;
    private List<SoftLock3> threads;
    private boolean running = true;

    private List<Lock> waiting = new ArrayList<>();
    private ArrayList<Lock>[] incoming;
    private long n;
    private int nextFree;
    private ArrayList<Lock> bigLocks;
    public SoftLock3(int id, int locksPerThread, int sublocks) {
        this.id = id;
        this.locksPerThread = locksPerThread;
        this.sublocks = sublocks;
        this.lock = new ReentrantReadWriteLock().writeLock();
        this.bigLocks = new ArrayList<Lock>();

    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        long seconds = 5;
        int locksSize = 100000;
        int sublocks = 10;
        int locksPerThread = locksSize / threadCount;
        List<SoftLock3> threads = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            SoftLock3 thread = new SoftLock3(x, locksPerThread, sublocks);
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
        System.out.println(String.format("Total full synchronizations %d", (locksSize * sublocks)));
        System.out.println(String.format("Locks across threads processed per second %d", (locksSize * sublocks) / (duration / 1000)));
        for (SoftLock3 thread : threads) {
            for (Lock lock : thread.bigLocks) {
                // System.out.println(lock.users);

            }
        }


    }

    private void setThreads(ArrayList<SoftLock3> threads) {
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

        while (running || waiting.size() > 0) {
            if (running && waiting.size() == 0) {
                for (int x = 0; x < threads.size(); x++) {
                    locks.put(x, new ArrayList<>());
                }
            for (int x = 0 ; x < locksPerThread; x++) {
                int pickedLock = rng.nextInt(threads.size());
                SoftLock3 target = threads.get(pickedLock);
                Lock currentLock = new Lock(pickedLock, this, target);
                bigLocks.add(currentLock);
                currentLock.addLock(currentLock);
                locks.get(pickedLock).add(currentLock);
                for (int n = 0 ; n < sublocks ; n++) {
                    int dependency = rng.nextInt(threads.size());
                    SoftLock3 dependencyThread = threads.get(pickedLock);
                    Lock dependencyLock = new Lock(dependency, this, dependencyThread);
                    locks.get(dependency).add(dependencyLock);
                    currentLock.addLock(dependencyLock);
                    waiting.add(dependencyLock);

                }

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
                if (!lock1.handled && lock1.parent.submitted == lock1.parent.locks.size()) {
                    n++;
                    lock1.handled = true;
                    removals.add(lock1);
                    // System.out.println(String.format("parent lock fulfilled %d", lock1.parent.submitted));


                }
            }
            waiting.removeAll(removals);
            removals.clear();
            for (int x = 0 ; x < threads.size() ; x++) {
                    List<Lock> remove = incoming[x];
                    threads.get(x).lock.lock();
                    for (int y = 0; y < remove.size(); y++) {
                        Lock item = remove.remove(y);
                        item.finish(id);
                    }
                    threads.get(x).lock.unlock();
            }

        }
    }


    private class Lock {
        public volatile boolean finished;
        public volatile boolean handled;
        private int pickedLock;
        private SoftLock3 owner;
        private SoftLock3 target;
        private ReentrantReadWriteLock.WriteLock lock;
        private int submitted;
        private List<Lock> locks;
        private Lock parent;
        private List<Integer> users;

        public Lock(int pickedLock, SoftLock3 owner, SoftLock3 target) {
            this.pickedLock = pickedLock;
            this.owner = owner;
            this.target = target;
            this.lock = new ReentrantReadWriteLock().writeLock();
            this.locks = new ArrayList<>();
            this.parent = this;
            this.users = new ArrayList<>();
        }

        public void addLock(Lock lock) {
            lock.parent = this;
            this.locks.add(lock);
        }

        public void finish(int id) {
            parent.addUser(id);
            parent.submitted++;
        }

        private void addUser(int id) {
            this.users.add(id);
        }

    }
}
