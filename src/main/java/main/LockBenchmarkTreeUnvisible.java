package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockBenchmarkTreeUnvisible extends Thread {
    private final HashMap<LockBenchmarkTreeUnvisible, Long> map;
    private final List<TreeObject> treeObjects;
    private LockBenchmarkTreeUnvisible main;

    private final int id;
    private volatile boolean buffer;
    private final Lock writeLock;
    private volatile boolean running = true;
    private TreeObject[] left;
    private TreeObject[] right;
    private DoublyLinkedList data;
    private String mode;
    private List<LockBenchmarkTreeUnvisible> threads;
    private long counter;
    private int lefts;
    private int rights;
    private ArrayList<TreeObject[]> sides;

    private List<HashMap<TreeObject, TreeObject>> equivalents;

    public LockBenchmarkTreeUnvisible(String mode, int id, Lock writeLock, List<TreeObject> treeObjects) {
        this.main = main;
        this.id = id;
        this.writeLock = writeLock;
        this.mode = mode;
        this.map = new HashMap<LockBenchmarkTreeUnvisible, Long>();
        if (main == null) {
            this.data = new DoublyLinkedList(0, System.currentTimeMillis());
            this.main = this;
        }
        this.treeObjects = treeObjects;
        left = new TreeObject[treeObjects.size()];
        right = new TreeObject[treeObjects.size()];
        sides = new ArrayList<>();
        equivalents = new ArrayList<>();
        sides.add(left);
        sides.add(right);
        for (TreeObject[] side : sides) {
            HashMap<TreeObject, TreeObject> equivalent = new HashMap<>();
            HashMap<TreeObject, TreeObject> reverse = new HashMap<>();
            equivalents.add(equivalent);

            for (int i = 0 ; i < treeObjects.size(); i++) {
                TreeObject mineNew = new TreeObject(treeObjects.get(i).id);
                equivalent.put(mineNew, treeObjects.get(i));
                reverse.put(treeObjects.get(i), mineNew);
                side[i] = mineNew;
            }
            for (TreeObject treeObject : side) {
                treeObject.left = reverse.get(equivalent.get(treeObject).left);
                treeObject.right = reverse.get(equivalent.get(treeObject).right);
            }
        }
    }

    public void run() {


        if (mode.equals("reader")) {
            while (running) {
                long total = 0;
                for (TreeObject treeObject : treeObjects) {
                    treeObject.count = 0;
                }
                for (LockBenchmarkTreeUnvisible thread : threads) {
                    if (thread.buffer) {
                        TreeObject[] left1 = thread.left;
                        total += thread.counter;
                        merge(thread, left1);
                        visible(thread);
                        thread.buffer = !thread.buffer;
                    } else {
                        TreeObject[] right1 = thread.right;
                        total += thread.counter;
                        merge(thread, right1);
                        visible(thread);
                        thread.buffer = !thread.buffer;
                    }
                }
                counter = total;
            }
        }
        SplittableRandom random = new SplittableRandom();
        if (mode.equals("counter")) {
            int size = left.length;
            int i = 0;
            while (running) {

                    while (buffer) {
                        i = random.nextInt(size);
                        left[i].count++;
                    }
                    while (!buffer) {
                        i = random.nextInt(size);
                        right[i].count++;
                    }

            }
        } else if (mode.equals("nolock")) {
            while (running) {
                counter += 1;
            }
        }
    }

    private void visible(LockBenchmarkTreeUnvisible thread) {
        if (thread.buffer) {
            for (int i = 0 ; i < treeObjects.size(); i++) {
                thread.right[i].count = thread.left[i].count;
            }
        } else {
            for (int i = 0 ; i < treeObjects.size(); i++) {
                thread.left[i].count = thread.right[i].count;
            }
        }
    }

    private void merge(LockBenchmarkTreeUnvisible thread,
                       TreeObject[] left1) {
        for (TreeObject treeObject : left1) {
            lookup(thread, treeObject).count += treeObject.count;
        }
    }

    private TreeObject lookup(LockBenchmarkTreeUnvisible thread, TreeObject treeObject) {
        if (thread.buffer) {
            return thread.equivalents.get(0).get(treeObject);
        } else {
            return thread.equivalents.get(1).get(treeObject);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        List<TreeObject> treeObjects = new ArrayList<>();
        TreeObject left3 = new TreeObject(1);
        treeObjects.add(left3);
        TreeObject right3 = new TreeObject(2);
        treeObjects.add(right3);
        TreeObject left2 = new TreeObject(3);
        treeObjects.add(left2);
        TreeObject right2 = new TreeObject(4);
        treeObjects.add(right2);
        TreeObject left1 = new TreeObject(5, left2, right2);
        treeObjects.add(left1);
        TreeObject right1 = new TreeObject(6, left3, right3);
        treeObjects.add(right1);
        TreeObject root = new TreeObject(7, left1, right1);
        treeObjects.add(root);

        List<LockBenchmarkTreeUnvisible> threads = new ArrayList<>();

        for (int i = 1 ; i < threadCount; i++) {
            ReadWriteLock rwlock = new ReentrantReadWriteLock();

            LockBenchmarkTreeUnvisible lockBenchmark = new LockBenchmarkTreeUnvisible("counter", i, rwlock.writeLock(), treeObjects);
            threads.add(lockBenchmark);
        }
        ReadWriteLock rwlock = new ReentrantReadWriteLock();

        LockBenchmarkTreeUnvisible reader = new LockBenchmarkTreeUnvisible("reader", 0, rwlock.writeLock(), treeObjects);
        reader.setThreads(threads);

        reader.start();
        long start = System.currentTimeMillis();
        for (LockBenchmarkTreeUnvisible loopBenchmark : threads) {
            loopBenchmark.start();
        }
        Thread.sleep(10000);
        reader.running = false;

        reader.join();
        Thread.sleep(100);
        for (LockBenchmarkTreeUnvisible loopBenchmark : threads) {
            loopBenchmark.running = false;
            loopBenchmark.buffer = !loopBenchmark.buffer;
        }
        for (LockBenchmarkTreeUnvisible loopBenchmark : threads) {
            loopBenchmark.join();
        }

        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        long totalRequests = 0;

        for (TreeObject treeObject : treeObjects) {
            totalRequests += treeObject.count;
        }

        long totalRequests2 = 0;
        for (TreeObject treeObject : treeObjects) {
            System.out.println(treeObject);
        }


        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }

    private void setThreads(List<LockBenchmarkTreeUnvisible> threads) {
        this.threads = threads;
    }

    private static class TreeObject {
        public long count;
        private TreeObject left;
        private TreeObject right;
        private int id;

        public TreeObject(TreeObject left, TreeObject right) {
            this.left = left;
            this.right = right;
        }
        public TreeObject(int id, TreeObject left, TreeObject right) {
            this.id = id;
            this.left = left;
            this.right = right;
        }

        public TreeObject(int id) {

            this.id = id;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(id);
            stringBuilder.append("\t");
            if (left != null) {
                stringBuilder.append(left.id);
            } else {
                stringBuilder.append("(empty)");
            }
            stringBuilder.append("\n");
            stringBuilder.append("\t");
            if (right != null) {
                stringBuilder.append(right.id);
            } else {
                stringBuilder.append("(empty)");
            }
            stringBuilder.append("\n");
            return stringBuilder.toString();
        }
    }
}
