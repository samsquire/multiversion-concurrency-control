package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Synchronizer4 extends Thread {
    private int data;
    public boolean synchronizer = false;
    private int committed = -1;
    private int id;
    private List<Synchronizer4> threads;
    private int threadsSize;
    private volatile boolean running = true;
    private long max = 0;
    private Lock[] writeLocks;
    private Lock[] readLocks;
    private Lock myLock;

    public Synchronizer4(int id, int size, boolean synchronizer) {
        this.id = id;
        this.data = 0;
        this.synchronizer = synchronizer;
        this.myLock = myLock;
    }

    public static void main(String[] args) throws InterruptedException {
        int threadsCount = 11;
        int size = 1000000000;
        List<Synchronizer4> threads = new ArrayList<>();
        int id = 0;
        Synchronizer4 synchronizer = new Synchronizer4(id++, size, true);
        threads.add(synchronizer);
        for (int i = 0 ; i < threadsCount; i++) {
            Synchronizer4 synchronizer4 = new Synchronizer4(id++, size, false);
            threads.add(synchronizer4);
        }
        for (int i = 0 ; i < threadsCount; i++) {
            threads.get(i).setThreads(synchronizer, new ArrayList<>(threads));
        }
        for (int i = 0 ; i < threadsCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(5000);
        for (int i = 0 ; i < threadsCount; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0 ; i < threadsCount; i++) {
            threads.get(i).join();
        }
        long end = System.currentTimeMillis();
        long totalRequests = 0;
        for (int i = 0; i < threads.size(); i++) {
            totalRequests += threads.get(i).data;
        }

        double seconds = (end - start) / 1000.0;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }

    private void setThreads(Synchronizer4 synchronizerThread, List<Synchronizer4> threads) {
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
            myLock = synchronizerThread.readLocks[id];
        }
    }

    public void run() {

        while (running) {
           if (synchronizer) {
               boolean allStopped = true;
               for (int i = 0; i < readLocks.length; i++) {
                   boolean locked = this.readLocks[i].tryLock();
                   if (!locked) {

                       for (int j = i; j >= 0; j--) {
                           this.readLocks[j].unlock();
                       }
                       allStopped = false;
                       break;

                   }

               }
               if (allStopped) {
                   for (int i = 0 ; i < this.threadsSize; i++) {
                       this.threads.get(i).exclusive();
                   }
                   for (int i = 0; i < this.readLocks.length; i++) {
                       this.readLocks[i].unlock();
                   }
               }
           } else {
               // counting thread
               myLock.lock();
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               this.data = this.data + 1;
               myLock.unlock();
           }

        }
    }

    private void exclusive() {

    }
}
