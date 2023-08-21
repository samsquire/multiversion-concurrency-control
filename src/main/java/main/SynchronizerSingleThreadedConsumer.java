package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Math.min;

public class SynchronizerSingleThreadedConsumer extends Thread {
    private static final int REACHED = 1;
    private static final int READY = 0;
    private static final int CLAIMED = 3;
    private static final int WAITING = 4;
    private static final int CLAIMING = 5;
    private static final int ACKNOWLEDGED = 6;


    private final int id;
    private List<List<BarrierTask>> left;
    private List<List<BarrierTask>> right;
    private volatile int mode;
    private final int LEFT = 0;
    private final int RIGHT = 1;
    private List<ReentrantReadWriteLock.WriteLock> locks = new ArrayList<>();
    private final boolean synchronizer;
    private int threadCount;
    private List<BarrierTask> clq;
    private volatile boolean running = true;
    private List<BarrierTask> tasks = new ArrayList<>();
    private List<SynchronizerSingleThreadedConsumer> threads;
    private volatile boolean parked = false;


    public SynchronizerSingleThreadedConsumer(int id,
                                              List<BarrierTask> clq,
                                              List<List<BarrierTask>> lefts,
                                              List<List<BarrierTask>> rights, List<ReentrantReadWriteLock.WriteLock> locks, boolean synchronizer,
                                              int threadCount) {
        this.id = id;
        this.clq = clq;
        this.left = lefts;
        this.right = rights;
        this.locks = locks;
        this.synchronizer = synchronizer;
        this.threadCount = threadCount;
    }

    public static void main(String args[]) throws InterruptedException {
        int threadCount = 11;
        int seconds = 5;
        List<List<BarrierTask>> lefts = new ArrayList<>();
        List<List<BarrierTask>> rights = new ArrayList<>();
        List<ReentrantReadWriteLock.WriteLock> locks = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            List<BarrierTask> clq = new ArrayList<>();
            List<BarrierTask> clq2 = new ArrayList<>();
            locks.add(new ReentrantReadWriteLock().writeLock());
            lefts.add(clq);
            rights.add(clq2);

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
        SynchronizerSingleThreadedConsumer synchronizer = new SynchronizerSingleThreadedConsumer(-1, null, lefts, rights, locks, true, threadCount);
        List<SynchronizerSingleThreadedConsumer> threads = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            SynchronizerSingleThreadedConsumer thread = new SynchronizerSingleThreadedConsumer(x, rights.get(x), lefts, rights, locks, false, threadCount);
            threads.add(thread);
            thread.addTask(tasks.get(x));
        }
        synchronizer.setThreads(threads);
        synchronizer.start();
        for (SynchronizerSingleThreadedConsumer thread : threads) {
            thread.start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(secondsMillis);
        for (SynchronizerSingleThreadedConsumer thread : threads) {
            thread.running = false;
        }
        synchronizer.running = false;
        for (SynchronizerSingleThreadedConsumer thread : threads) {
            thread.join();
        }
        synchronizer.join();
        long end = System.currentTimeMillis();

        long n = 0;
        for (BarrierTask task : tasks) {
            n += task.n;
        }
        long b = 0;
        for (BarrierTask task : tasks) {
            b += task.b;
        }
        long elapsed = (end - start) / 1000;
        System.out.println(String.format("Requests per second %d", n / elapsed));
        System.out.println(String.format("Unsynchronized requests per second %d", b / elapsed));
        System.out.println("Finished");
    }

    private void setThreads(List<SynchronizerSingleThreadedConsumer> threads) {
        this.threads = threads;
    }

    private void addTask(BarrierTask barrierTask) {
        this.tasks.add(barrierTask);
    }

    public void run() {
        if (synchronizer) {
            while (running) {
                int empty = 0;
                for (int x = 0; x < threadCount; x++) {
                    int mode = threads.get(x).mode;
                            if (mode == LEFT) {
                                int size = threads.get(x).left.get(x).size();
                                for (int y = 0; y < min(size, 10000); y++) {
                                    BarrierTask item = threads.get(x).left.get(x).remove(threads.get(x).left.get(x).size() - 1);
                                    if (item != null) {
                                        item.run();
                                    }
                                }
                                threads.get(x).mode = RIGHT;

                            }
                            else if (mode == RIGHT) {
                                    int size = threads.get(x).right.get(x).size();
                                    for (int y = 0; y < min(size, 10000); y++) {

                                        BarrierTask item = threads.get(x).right.get(x).remove(
                                                threads.get(x).right.get(x).size() - 1);
                                        if (item != null) {
                                            item.run();
                                        }
                                    }
                                threads.get(x).mode = LEFT;

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
                            int myMode = mode;
                            if (myMode == LEFT) {
                                right.get(id).add(task);
                            } else {
                                left.get(id).add(task);
                            }
//                        System.out.println(String.format("Posted message %d", id));
                    }
    //                      System.out.println("Adding item");


                        // parked = true;


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
