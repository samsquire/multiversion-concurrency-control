package main;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Math.max;

public class Synchronizer4DLL extends Thread {
    private long[] data;
    private long start;
    private long max;

    private long total = 0;

    public boolean synchronizer = false;
    private int committed = -1;
    private int id;
    private List<Synchronizer4DLL> threads;
    private int threadsSize;
    private volatile boolean running = true;
    private Lock[] writeLocks;
    private Lock[] readLocks;
    private Lock myLock;
    private int synchronizations = 0;
    private int size;
    private long limit;
    private long previous = 0;
    private long next_max;
    private long next_limit;
    private boolean needsAdjustment = false;
    private DoublyLinkedList dll;

    public Synchronizer4DLL(DoublyLinkedList dll, int start, int id, int size, boolean synchronizer) {
        this.dll = dll;
        System.out.println(String.format("Start is %d", start));
        this.start = start;
        this.size = size;
        this.id = id;
        this.data = new long[size];
        Arrays.fill(this.data, -1);
        this.synchronizer = synchronizer;
    }

    public static void main(String[] args) throws InterruptedException {
        int threadsCount = 11;
        DoublyLinkedList dll = new DoublyLinkedList(0, 0);
        int size = 10000000;
        List<Synchronizer4DLL> threads = new ArrayList<>();
        int id = 0;
        int indexStart = 0;
        Synchronizer4DLL synchronizer = new Synchronizer4DLL(dll, indexStart, id++, size, true);
        threads.add(synchronizer);
        for (int i = 0; i < threadsCount; i++) {

            Synchronizer4DLL synchronizer4 = new Synchronizer4DLL(dll, indexStart, id++, size, false);
            threads.add(synchronizer4);
            indexStart += size;
        }
        for (int i = 0; i < threadsCount; i++) {
            threads.get(i).setThreads(synchronizer, new ArrayList<>(threads));
        }
        for (int i = 0; i < threadsCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(5000);
        for (int i = 0; i < threadsCount; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0; i < threadsCount; i++) {
            threads.get(i).join();
        }
        long end = System.currentTimeMillis();
        long totalRequests = 0;
        for (int i = 0; i < threads.size(); i++) {
            totalRequests += threads.get(i).total;
        }
        long totalSynchronizations = 0;
        for (int i = 0; i < threads.size(); i++) {
            totalSynchronizations += threads.get(i).synchronizations;
        }

        double seconds = (end - start) / 1000.0;
        System.out.println(String.format("%d total requests", totalRequests));
        System.out.println(String.format("%d total synchronizations", totalSynchronizations));
        double l = totalRequests / seconds;
        double s = totalSynchronizations / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("%f synchronizations per second", s));
        Map<Long, Boolean> found = new HashMap<>();
//        System.out.println("Doing duplicate check");
//        for (int i = 1; i < threads.size(); i++) {
//            for (int j = 0; j < threads.get(i).data.length; j++) {
//                //System.out.println(threads.get(i).data[j]);
//                if (threads.get(i).data[j] == -1) { break; }
//                assert !found.containsKey(threads.get(i).data[j]) :threads.get(i).data[j];
//                found.put(threads.get(i).data[j], true);
//            }
//
//        }
//        found = null;
//        System.out.println("Doing hole check");
//        List<Long> data = new ArrayList<>();
//        for (int i = 1; i < threads.size(); i++) {
//            for (int j = 0; j < threads.get(i).data.length; j++) {
//                if (threads.get(i).data[j] != -1) {
//                    data.add(threads.get(i).data[j]);
//                }
//            }
//        }
//        System.out.println(data.size());
//        data.sort(Long::compare);
//        for (int i = 0 ; i < 10; i++) {
//            System.out.println(data.get(i));
//        }
//        for (int i = 1; i < data.size(); i++) {
//            long difference = data.get(i) - data.get(i - 1);
////            System.out.println(String.format("%d %d", data.get(i), data.get(i - 1)));
//            assert difference == 1 : difference;
//        }
        System.out.println(String.format("Time taken: %f", seconds));


    }

    private void setThreads(Synchronizer4DLL synchronizerThread, List<Synchronizer4DLL> threads) {
        this.threads = threads;
        this.threadsSize = threads.size();
        this.readLocks = new Lock[this.threadsSize];
        this.writeLocks = new Lock[this.threadsSize];
        if (this.synchronizer) {


            for (int i = 0; i < this.threadsSize; i++) {
                ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
                this.writeLocks[i] = reentrantReadWriteLock.writeLock();
                this.readLocks[i] = reentrantReadWriteLock.readLock();
            }
        } else {
            myLock = synchronizerThread.writeLocks[id];
        }
    }

    public void run() {
        this.committed = 0;
        this.previous = start;
        this.max = start;

        this.limit = start + size - 1;
        this.data[this.committed] = (int) start;
        while (running && !(committed >= size - 1)) {
            if (synchronizer) {
                boolean allStopped = true;
                for (int i = 0; i < writeLocks.length; i++) {
                    boolean locked = this.writeLocks[i].tryLock();
                    if (!locked) {

                        for (int j = i - 1; j >= 0; j--) {
                            this.writeLocks[j].unlock();
                        }
                        allStopped = false;
                        break;

                    }

                }
                if (allStopped) {
                    for (int i = 0; i < this.threadsSize; i++) {
                        this.threads.get(i).exclusive();
                    }

                    int previous = 0;
                    List<Integer> toSchedule = new ArrayList<>();
                    long last_max = threads.get(0).max;
                    for (int i = 1; i < this.threadsSize - 1; i++) {
                        long difference = this.threads.get(i).start - this.threads.get(previous).max;
                        if (difference > 1 && difference != size) {
//                            System.out.println(String.format("Hole found %d", difference));
                            long originalMax = this.threads.get(i).max;
                            // this.threads.get(i).needsAdjustment = true;
                            this.threads.get(i).next_max = this.threads.get(i - 1).max;
                            this.threads.get(i).next_limit = originalMax - 1;

                            toSchedule.add(previous);
                        }
                        last_max = max(this.threads.get(i).max, last_max);
                        previous = i;
                    }
//                    for (int i = 1; i < this.threadsSize -1; i++) {
//                        if (this.threads.get(i).needsAdjustment) {
//                            this.threads.get(i).needsAdjustment = false;
//                            this.threads.get(i).max = this.threads.get(i).next_max;
//                            this.threads.get(i).limit = this.threads.get(i).next_limit;
//                        }
//                    }
                        for (int i = 0; i < this.threadsSize; i++) {
                            if (this.threads.get(i).committed >= size - 1) {
                                this.threads.get(i).committed = 0;
                            }
                        }
//                    long newStart = last_max;

//                    for (Integer thread : toSchedule) {
//
//                        this.threads.get(thread).max = newStart;
//                        this.threads.get(thread).limit = newStart + size;
//                        newStart += size;
//                    }
//                   for (int i = 0 ; i < this.threadsSize; i++) {
//                       threads.get(i).committed = 0;
//                   }

                    for (int i = 0; i < this.readLocks.length; i++) {
                        this.writeLocks[i].unlock();
                    }
                    // Thread.yield();
                }
            } else {
                if (committed >= size - 1 || max >= limit) {
                    continue;
                }
                // counting thread
                int next = committed + 1;
                myLock.lock();
                this.data[next] = max + 1;
                myLock.unlock();
                committed = next;




                max = this.data[committed];
                total += 1;

            }

        }
    }

    private void exclusive() {
        synchronizations++;
        dll.insert(0);
    }
}
