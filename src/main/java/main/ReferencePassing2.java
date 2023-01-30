package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReferencePassing2 extends Thread {
    private final ReentrantReadWriteLock lock;
    private List<ReferencePassing2> threads;
    private boolean running = true;
    private long count = 0;
    private DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());

    private DoublyLinkedList incoming = null;
    private ReferencePassing2 owner;

    public ReferencePassing2() {
        this.lock = new ReentrantReadWriteLock();
    }
    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        List<ReferencePassing2> threads = new ArrayList<>();
        for (int i = 0 ; i < threadCount; i++) {
            threads.add(new ReferencePassing2());
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }

        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).join();
        }
        System.out.println("Finished");
        long end = System.currentTimeMillis();

        long totalRequests = 0;

        for (int i = 0; i < threads.size(); i++) {
            totalRequests += threads.get(i).count;
        }

        double seconds = (end - start) / 1000.0;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }

    private void setThreads(ArrayList<ReferencePassing2> threads) {
        this.threads = threads;
    }

    public void run() {
        Random rng = new Random();
        while (running) {
            lock();
            if (incoming != null) {
//                System.out.println("Receiving another thread's data");
                incoming.insert(incoming.value + 1);
                count++;
                owner.data = incoming;
//                System.out.println("Returning reference to other thread");
                incoming = null;
            }
            unlock();
            if (data == null) {
                continue;
            }
            ReferencePassing2 other = threads.get(rng.nextInt(threads.size()));
            other.lock();
            if (other.incoming == null) {
                other.owner = this;
                other.incoming = data;
                data = null;

            }
            other.unlock();
        }
    }

    private void unlock() {
        this.lock.writeLock().unlock();
    }

    private void lock() {
        this.lock.writeLock().lock();
    }
}
