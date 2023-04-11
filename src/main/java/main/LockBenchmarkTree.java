package main;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockBenchmarkTree extends Thread {
    private volatile boolean finishedReading;

    private final HashMap<LockBenchmarkTree, Long> map;
    private final List<TreeObject> treeObjects;
    private LockBenchmarkTree main;

    private final int id;
    private volatile boolean buffer;
    private volatile boolean globalBuffer;
    private final Lock writeLock;
    private volatile boolean running = true;
    private volatile TreeObject[] left;
    private volatile TreeObject[] right;
    private DoublyLinkedList data;
    private String mode;
    private List<LockBenchmarkTree> threads;
    private long counter;
    private int lefts;
    private int rights;
    private ArrayList<TreeObject[]> sides;

    private ArrayList<TreeObject[]> threadDataLeft;
    private ArrayList<TreeObject[]> threadDataRight;

    private List<HashMap<TreeObject, TreeObject>> equivalents;
    private List<List<HashMap<TreeObject, TreeObject>>> threadEquivalents;
    private ArrayList<ArrayList<TreeObject[]>> threadSides;
    private LockBenchmarkTree reader;

    public LockBenchmarkTree(String mode,
                             int id,
                             Lock writeLock,
                             List<TreeObject> treeObjects,
                             LockBenchmarkTree reader) {
        this.reader = reader;
        this.main = main;
        this.id = id;
        this.writeLock = writeLock;
        this.mode = mode;
        this.map = new HashMap<LockBenchmarkTree, Long>();
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
            threadDataLeft = new ArrayList<TreeObject[]>();
            threadDataRight = new ArrayList<TreeObject[]>();
            threadSides = new ArrayList<>();
            threadSides.add(threadDataLeft);
            threadSides.add(threadDataRight);
            for (ArrayList<TreeObject[]> side : threadSides) {
                ArrayList<HashMap<TreeObject, TreeObject>> thisThreadEquivalents = new ArrayList<>();

                for (LockBenchmarkTree tree : threads) {
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


                if (buffer) {
                    for (TreeObject treeObject : left) {
                        treeObject.count = 0;
                    }
                } else {
                    for (TreeObject treeObject : right) {
                        treeObject.count = 0;
                    }
                }
                for (TreeObject treeObject : treeObjects) {
                    treeObject.count = 0;
                }
                for (ArrayList<TreeObject[]> side : threadSides) {
                    for (int thread = 0; thread < side.size(); thread++) {
                        if (threads.get(thread).globalBuffer) {
                            for (TreeObject treeObject : side.get(thread)) {
                                treeObject.count = 0;
                            }
                        } else {

                        }
                    }
                }
                for (LockBenchmarkTree thread : threads) {
                    if (thread.buffer) {
                        TreeObject[] left1 = thread.left;
                        total += thread.counter;
                        merge(true, thread, left1);
                        if (thread.globalBuffer) {
                            mergePerThread(false, this, thread, left1);
                        } else {
                            mergePerThread(true, this, thread, left1);
                        }



                        visible(thread);
                        thread.buffer = !thread.buffer;
                    } else {
                        TreeObject[] right1 = thread.right;
                        total += thread.counter;
                        merge(false, thread, right1);
                            if (thread.globalBuffer) {
                                mergePerThread(false, this, thread, right1);
                            } else {
                                mergePerThread(true, this, thread, right1);
                            }
                            // thread.globalBuffer = !thread.globalBuffer;



                        visible(thread);
                        thread.buffer = !thread.buffer;
                    }
                }

                counter = total;
                for (LockBenchmarkTree thread2 : threads) {
                    if (thread2.finishedReading) {
                        thread2.finishedReading = false;

                    }

                }
            }
        }
        SplittableRandom random = new SplittableRandom();
        if (mode.equals("counter")) {
            int size = left.length;
            int i = 0;
            while (running) {
                int modCount = 0;
                int modCount2 = 0;
                for (TreeObject treeObject : reader.threadSides.get(0).get(id)) {
                    modCount += treeObject.count;
                }
                for (TreeObject treeObject : reader.threadSides.get(1).get(id)) {
                    modCount2 += treeObject.count;
                }
                    while (this.buffer) {
                        i = random.nextInt(size);
                        left[i].count++;
                    }

                    while (!this.buffer) {
                        i = random.nextInt(size);
                        right[i].count++;
                    }
//                    if (globalBuffer) {
//                            System.out.println(reader.threadDataRight.get(id)[0].count);
//                    } else {
//                            System.out.println(reader.threadDataLeft.get(id)[0].count);
//                    }
                    finishedReading = true;
                    int newModCount = 0;
                    for (TreeObject treeObject : reader.threadSides.get(0).get(id)) {
                        newModCount += treeObject.count;
                    }
                    int newModCount2 = 0;
                    for (TreeObject treeObject : reader.threadSides.get(1).get(id)) {
                        newModCount2 += treeObject.count;
                    }
                    assert newModCount == modCount;
                    assert newModCount2 == modCount2;
                    globalBuffer = !globalBuffer;

            }

        } else if (mode.equals("nolock")) {
            while (running) {
                counter += 1;
            }
        }
    }

    private void mergePerThread(boolean side, LockBenchmarkTree thread, LockBenchmarkTree targetThread, TreeObject[] objects) {
        if (side) {
            for (int x = 0 ; x < treeObjects.size(); x++) {
                threadSides.get(0).get(targetThread.id)[x].count += objects[x].count;
            }
        } else {
            for (int x = 0 ; x < treeObjects.size(); x++) {
                threadSides.get(1).get(targetThread.id)[x].count += objects[x].count;
            }
        }

    }
    private void visible(LockBenchmarkTree thread) {
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

    private void merge(boolean side, LockBenchmarkTree thread,
                       TreeObject[] left1) {
        for (TreeObject treeObject : left1) {
            lookup(side, thread, treeObject).count += treeObject.count;
        }
    }

    private TreeObject lookup(boolean side, LockBenchmarkTree thread, TreeObject treeObject) {
        if (side) {
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

        List<LockBenchmarkTree> threads = new ArrayList<>();

        ReadWriteLock rwlock = new ReentrantReadWriteLock();

        LockBenchmarkTree reader = new LockBenchmarkTree("reader", threadCount, rwlock.writeLock(), treeObjects, null);
        for (int i = 0 ; i < threadCount; i++) {
            rwlock = new ReentrantReadWriteLock();

            LockBenchmarkTree lockBenchmark = new LockBenchmarkTree("counter", i, rwlock.writeLock(), treeObjects, reader);
            threads.add(lockBenchmark);
        }
        reader.setThreads(new ArrayList<>(threads));

        reader.start();
        long start = System.currentTimeMillis();
        for (LockBenchmarkTree loopBenchmark : threads) {
            loopBenchmark.start();
        }
        Thread.sleep(10000);

        for (LockBenchmarkTree loopBenchmark : threads) {
            loopBenchmark.running = false;
            loopBenchmark.buffer = !loopBenchmark.buffer;
        }

        Thread.sleep(100);
        for (LockBenchmarkTree loopBenchmark : threads) {
            loopBenchmark.buffer = !loopBenchmark.buffer;
        }
        reader.running = false;
        reader.join();
        for (LockBenchmarkTree loopBenchmark : threads) {
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
        System.out.println(String.format("%d requests per second 2", threads.get(0).counter));
        System.out.println(String.format("Time taken: %f", seconds));
    }

    private void setThreads(List<LockBenchmarkTree> threads) {
        this.threads = threads;
    }

    private static class TreeObject {
        public long count;
        public int modCount = 0;
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
