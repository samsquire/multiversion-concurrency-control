package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockBenchmarkMultipleSeersSnapshot extends Thread {
    private final HashMap<LockBenchmarkMultipleSeersSnapshot, Long> map;
    private final List<TreeObject> treeObjects;
    private LockBenchmarkMultipleSeersSnapshot main;

    private final int id;
    private volatile boolean buffer = false;
    private volatile boolean globalBuffer = false;
    private volatile Overseer overseer;
    private final Lock writeLock;
    private volatile boolean running = true;
    private volatile TreeObject[] left;
    private volatile TreeObject[] right;
    private DoublyLinkedList data;
    private String mode;
    private List<LockBenchmarkMultipleSeersSnapshot> threads;
    private long counter;
    private int lefts;
    private int rights;
    private ArrayList<TreeObject[]> sides;

    private ArrayList<TreeObject[]> snapshotLeft;
    private ArrayList<TreeObject[]> snapshotRight;

    private List<HashMap<TreeObject, TreeObject>> equivalents;
    private List<List<HashMap<TreeObject, TreeObject>>> threadEquivalents;
    private ArrayList<ArrayList<TreeObject[]>> snapshots;
    private LockBenchmarkMultipleSeersSnapshot reader;

    public LockBenchmarkMultipleSeersSnapshot(String mode,
                                              Overseer overseer, int id,
                                              Lock writeLock,
                                              List<TreeObject> treeObjects,
                                              LockBenchmarkMultipleSeersSnapshot reader) {
        this.overseer = overseer;
        this.reader = reader;
        this.main = main;
        this.id = id;
        this.writeLock = writeLock;
        this.mode = mode;
        this.map = new HashMap<LockBenchmarkMultipleSeersSnapshot, Long>();
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
        threadEquivalents = new ArrayList<>();
    }

    public void run() {


        if (mode.equals("reader")) {
            snapshotLeft = new ArrayList<TreeObject[]>();
            snapshotRight = new ArrayList<TreeObject[]>();
            snapshots = new ArrayList<>();
            snapshots.add(snapshotLeft);
            snapshots.add(snapshotRight);
            for (ArrayList<TreeObject[]> side : snapshots) {
                ArrayList<HashMap<TreeObject, TreeObject>> thisThreadEquivalents =
                        new ArrayList<>();

                for (LockBenchmarkMultipleSeersSnapshot tree : threads) {
                    HashMap<TreeObject, TreeObject> equivalent = new HashMap<>();
                    HashMap<TreeObject, TreeObject> reverse = new HashMap<>();
                    thisThreadEquivalents.add(equivalent);
                    TreeObject[] objects = new TreeObject[treeObjects.size()];
                    for (int i = 0; i < treeObjects.size(); i++) {
                        TreeObject mineNew = new TreeObject(treeObjects.get(i).id);
                        equivalent.put(mineNew, treeObjects.get(i));
                        reverse.put(treeObjects.get(i), mineNew);
                        objects[i] = mineNew;
                    }

                    for (TreeObject treeObject : objects) {
                        treeObject.left = reverse.get(equivalent.get(treeObject).left);
                        treeObject.right = reverse.get(equivalent.get(treeObject).right);
                    }
                    side.add(objects);
                }

                threadEquivalents.add(thisThreadEquivalents);

            }
            while (running) {
                long total = 0;

                for (LockBenchmarkMultipleSeersSnapshot thread : threads) {
                    if (thread.buffer) {
                        for (TreeObject treeObject : thread.right) {
                            treeObject.count = 0;
                        }
                    } else {
                        for (TreeObject treeObject : thread.left) {
                            treeObject.count = 0;
                        }
                    }

                    if (thread.buffer) {
                        TreeObject[] left1 = thread.left;
                        total += thread.counter;
                        merge(true, thread, left1);
                        mergePerThread(thread, left1);

                        visible(thread);
                        thread.buffer = !thread.buffer;


                    } else {
                        TreeObject[] right1 = thread.right;
                        total += thread.counter;
                        merge(false, thread, right1);
                        mergePerThread(thread, right1);



                        visible(thread);
                        thread.buffer = !thread.buffer;



                    }
                }


                counter = total;

            }
            System.out.println("Finished reader");
        }

        SplittableRandom random = new SplittableRandom();
        if (mode.equals("counter")) {
            int size = left.length;
            int i = 0;
            while (running) {

                long modCount = 0;
                boolean thisGlobal = globalBuffer;
                if (thisGlobal) {
                    for (TreeObject treeObject : reader.snapshots.get(1).get(id)) {
                        modCount += treeObject.count;
                    }
                } else {
                    for (TreeObject treeObject : reader.snapshots.get(0).get(id)) {
                        modCount += treeObject.count;
                    }
                }
                while (this.buffer) {

                        i = random.nextInt(size);
                        right[i].count++;
                        assert reader.snapshots.get(1).get(id)[0] != reader.snapshots.get(0).get(id)[0];

                    }
                long newModCount = 0;
                if (thisGlobal) {
                    for (TreeObject treeObject : reader.snapshots.get(1).get(id)) {
                        newModCount += treeObject.count;
                    }
                    // assert newModCount == modCount : String.format("%d %d", modCount, newModCount);
                    System.out.println(String.format("%d %d", modCount, newModCount));
                } else {
                    for (TreeObject treeObject : reader.snapshots.get(0).get(id)) {
                        newModCount += treeObject.count;
                    }

                    assert newModCount == modCount : String.format("%d %d", newModCount, modCount);
                    System.out.println(String.format("%d %d", modCount, newModCount));
                }

                    while (!this.buffer) {
                        i = random.nextInt(size);
                        left[i].count++;

                    }
//                    if (globalBuffer.get()) {
//                            System.out.println(reader.snapshots.get(1).get(id)[1].count);
//                    } else {
//                            System.out.println(reader.snapshots.get(0).get(id)[0].count);
//                    }




                 this.globalBuffer = !globalBuffer;
            }
            System.out.println("Finished counter");
        } else if (mode.equals("nolock")) {
            while (running) {
                counter += 1;
            }
        }
    }

    private void mergePerThread(LockBenchmarkMultipleSeersSnapshot targetThread,
                                TreeObject[] objects) {


        if (targetThread.globalBuffer) {
            for (int x = 0 ; x < treeObjects.size(); x++) {
                //snapshots.get(0).get(targetThread.id)[x].count = snapshots.get(1).get(targetThread.id)[x].count;
            }
            for (int x = 0 ; x < treeObjects.size(); x++) {
               // snapshots.get(0).get(targetThread.id)[x].count += objects[x].count;
            }
        } else {
            for (int x = 0 ; x < treeObjects.size(); x++) {
                //snapshots.get(1).get(targetThread.id)[x].count = snapshots.get(0).get(targetThread.id)[x].count;
            }
            for (int x = 0 ; x < treeObjects.size(); x++) {
                // snapshots.get(1).get(targetThread.id)[x].count += objects[x].count;
            }

        }

    }
    private void visible(LockBenchmarkMultipleSeersSnapshot thread) {
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

    private void merge(boolean side, LockBenchmarkMultipleSeersSnapshot thread,
                       TreeObject[] left1) {
        for (TreeObject treeObject : left1) {
            lookup(side, thread, treeObject).count += treeObject.count;
        }
    }

    private TreeObject lookup(boolean side,
                              LockBenchmarkMultipleSeersSnapshot thread,
                              TreeObject treeObject) {
        if (side) {
            return thread.equivalents.get(0).get(treeObject);
        } else {
            return thread.equivalents.get(1).get(treeObject);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        int seerThreadsCount = 1;
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

        List<LockBenchmarkMultipleSeersSnapshot> allThreads = new ArrayList<>();
        List<LockBenchmarkMultipleSeersSnapshot> seerThreads = new ArrayList<>();
        List<LockBenchmarkMultipleSeersSnapshot> workerThreads = new ArrayList<>();

        ReadWriteLock rwlock = new ReentrantReadWriteLock();
        Overseer overseer = new Overseer(0, seerThreadsCount);
        for (int i = 0 ; i < seerThreadsCount; i++) {
            List<LockBenchmarkMultipleSeersSnapshot> threads = new ArrayList<>();
            LockBenchmarkMultipleSeersSnapshot reader = new LockBenchmarkMultipleSeersSnapshot("reader", overseer, i, rwlock.writeLock(), treeObjects, null);
            allThreads.add(reader);
            seerThreads.add(reader);
            for (int j = 0 ; j < threadCount; j++) {
                rwlock = new ReentrantReadWriteLock();

                LockBenchmarkMultipleSeersSnapshot lockBenchmark = new LockBenchmarkMultipleSeersSnapshot("counter", overseer, j, rwlock.writeLock(), treeObjects, reader);
                threads.add(lockBenchmark);
                allThreads.add(lockBenchmark);
                workerThreads.add(lockBenchmark);
            }
            seerThreads.get(i).setThreads(new ArrayList<>(threads));
            seerThreads.get(i).start();
        }

        Thread.sleep(100);
        long start = System.currentTimeMillis();
        for (LockBenchmarkMultipleSeersSnapshot loopBenchmark : workerThreads) {
            loopBenchmark.start();
        }
        Thread.sleep(10000);
        System.out.println("test over");
        for (LockBenchmarkMultipleSeersSnapshot loopBenchmark : allThreads) {
            loopBenchmark.running = false;
            loopBenchmark.buffer = !loopBenchmark.buffer;

        }

        Thread.sleep(100);
        for (LockBenchmarkMultipleSeersSnapshot loopBenchmark : allThreads) {
            loopBenchmark.buffer = !loopBenchmark.buffer;

        }
        Thread.sleep(100);
        for (LockBenchmarkMultipleSeersSnapshot loopBenchmark : allThreads) {
            loopBenchmark.buffer = !loopBenchmark.buffer;
        }

        for (LockBenchmarkMultipleSeersSnapshot loopBenchmark : allThreads) {
            loopBenchmark.join();
        }


        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        long totalRequests = 0;

        for (TreeObject treeObject : treeObjects) {
            totalRequests += treeObject.count;
        }

        for (TreeObject treeObject : treeObjects) {
            System.out.println(treeObject);
        }


        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }

    private void setThreads(List<LockBenchmarkMultipleSeersSnapshot> threads) {
        this.threads = threads;
    }

    private static class TreeObject {
        public volatile long count;

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

    private static class Overseer {
        private final int count;
        volatile int id;
        public Overseer(int id, int count) { this.id = id; this.count = count; }
    }
}
