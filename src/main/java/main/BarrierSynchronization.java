package main;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BarrierSynchronization extends Thread {
    private static final int REACHED = 1;
    private static final int READY = 0;
    private static final int CLAIMED = 3;
    private static final int WAITING = 4;
    private static final int CLAIMING = 5;
    private static final int ACKNOWLEDGED = 6;


    private final int id;
    private final boolean synchronizer;
    private ConcurrentLinkedQueue<BarrierTask> clq;
    private volatile boolean running = true;
    private List<BarrierTask> tasks = new ArrayList<>();


    public BarrierSynchronization(int id,
                                  ConcurrentLinkedQueue<BarrierTask> clq,
                                  boolean synchronizer) {
        this.id = id;
        this.clq = clq;
        this.synchronizer = synchronizer;
    }

    public static void main(String args[]) throws InterruptedException {
        int threadCount = 12;
        int seconds = 5;
        ConcurrentLinkedQueue<BarrierTask> clq = new ConcurrentLinkedQueue<>();
        int secondsMillis = seconds * 1000;
        DoublyLinkedList dll = new DoublyLinkedList(0, 100);
        BarrierTask parent = new BarrierTask(null, threadCount, -1, threadCount);
        parent.data = dll;
        ArrayList<BarrierTask> tasks = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            BarrierTask task = new BarrierTask(parent, threadCount, x, threadCount);
            tasks.add(task);
        }
        BarrierSynchronization synchronizer = new BarrierSynchronization(-1, clq, true);
        synchronizer.start();
        List<BarrierSynchronization> threads = new ArrayList();
        for (int x = 0; x < threadCount; x++) {
            BarrierSynchronization thread = new BarrierSynchronization(x, clq, false);
            threads.add(thread);
            thread.addTask(tasks.get(x));
        }
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

    private void addTask(BarrierTask barrierTask) {
        this.tasks.add(barrierTask);
    }

    public void run() {
        if (synchronizer) {
            while (running) {
                BarrierTask item = clq.poll();
                if (item != null) {
                    item.run();
                } else {
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
                    } else {

                        // task.run();
                        // task.parent.status[id]++;
                    }

                }
            }
        }
    }

    private boolean ready(BarrierTask task) {
        return true;
    }

    private boolean canRun(BarrierTask task) {
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
