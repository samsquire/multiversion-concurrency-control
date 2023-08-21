package main;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class BarrierSynchronization extends Thread {
    private static final int REACHED = 1;
    private static final int READY = 0;
    private static final int CLAIMED = 3;
    private static final int WAITING = 4;
    private static final int CLAIMING = 5;
    private static final int ACKNOWLEDGED = 6;


    private final int id;
    private List<ConcurrentLinkedQueue<BarrierTask>> clqs;
    private final boolean synchronizer;
    private int threadCount;
    private ConcurrentLinkedQueue<BarrierTask> clq;
    private volatile boolean running = true;
    private List<BarrierTask> tasks = new ArrayList<>();
    private List<BarrierSynchronization> threads;
    private volatile boolean parked;


    public BarrierSynchronization(int id,
                                  ConcurrentLinkedQueue<BarrierTask> clq,
                                  List<ConcurrentLinkedQueue<BarrierTask>> clqs,
                                  boolean synchronizer,
                                  int threadCount) {
        this.id = id;
        this.clq = clq;
        this.clqs = clqs;
        this.synchronizer = synchronizer;
        this.threadCount = threadCount;
    }

    public static void main(String args[]) throws InterruptedException {
        int threadCount = 11;
        int seconds = 5;
        List<ConcurrentLinkedQueue<BarrierTask>> clqs = new ArrayList<>();
        for (int x = 0 ; x < threadCount; x++) {
            ConcurrentLinkedQueue<BarrierTask> clq = new ConcurrentLinkedQueue<>();
            clqs.add(clq);

        }
        int secondsMillis = seconds * 1000;
        DoublyLinkedList dll = new DoublyLinkedList(0, 100);
        BarrierTask parent = new BarrierTask(null, threadCount, -1, threadCount);
        parent.data = dll;
        ArrayList<BarrierTask> tasks = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            BarrierTask task = new BarrierTask(parent, threadCount, x, threadCount);
            tasks.add(task);
        }
        BarrierSynchronization synchronizer = new BarrierSynchronization(-1, null, clqs, true, threadCount);
        List<BarrierSynchronization> threads = new ArrayList();
        for (int x = 0; x < threadCount; x++) {
            BarrierSynchronization thread = new BarrierSynchronization(x, clqs.get(x), clqs, false, threadCount);
            threads.add(thread);
            thread.addTask(tasks.get(x));
        }
        synchronizer.setThreads(threads);
        synchronizer.start();
        for (BarrierSynchronization thread : threads) {
            thread.start();
        }
        Thread.sleep(secondsMillis);
        for (BarrierSynchronization thread : threads) {
            thread.running = false;
        }
        synchronizer.running = false;
        for (BarrierSynchronization thread : threads) {
            thread.join();
        }
        synchronizer.join();
        long n = 0;
        for (BarrierTask task : tasks) {
            n += task.n;
        }
        long b = 0;
        for (BarrierTask task : tasks) {
            b += task.b;
        }
        System.out.println(String.format("Requests per second %d", n / seconds));
        System.out.println(String.format("Unsynchronized requests per second %d", b / seconds));
        System.out.println("Finished");
    }

    private void setThreads(List<BarrierSynchronization> threads) {
        this.threads = threads;
    }

    private void addTask(BarrierTask barrierTask) {
        this.tasks.add(barrierTask);
    }

    public void run() {
        if (synchronizer) {
            while (running) {
                int empty = 0;
                for (ConcurrentLinkedQueue<BarrierTask> clq : clqs) {
                    BarrierTask item = clq.poll();
                    if (item != null) {
                        item.run();
                    } else {
                        empty++;
                    }
                }
                if (empty == threadCount) {
                    for (BarrierSynchronization thread : threads) {
                        thread.parked = false;
                    }
                }
            }
        } else {
            while (running) {

                for (BarrierTask task : tasks) {


                    if (ready(task)) {
                        task.ready();
                    }

                    if (canRun(task)) {
                        clq.offer(task);
                        parked = true;
                    } else {

                        // task.run();
                        // task.parent.status[id]++;
                    }

                }
            }
        }
    }

    private boolean canRun(BarrierTask task) {
        return !parked;
    }

    private boolean ready(BarrierTask task) {
        return true;
    }

    private static class BarrierTask {


        private final BarrierTask parent;
        public DoublyLinkedList data;
        private volatile int status[];
        private AtomicInteger activeTask = new AtomicInteger();

        private int id = 0;
        private int n;
        private int threadsSize;
        private int b;


        public BarrierTask(BarrierTask parent, int size, int id, int threadsSize) {
            this.parent = parent;
            this.status = new int[size];
            this.id = id;
            this.threadsSize = threadsSize;
        }

        public void run() {
            parent.data.insert(n);
            n++;
        }

        public void ready() {
            // System.out.println(String.format("%d Ready", id));
            this.parent.status[id] = READY;
            b++;
        }
    }
}
