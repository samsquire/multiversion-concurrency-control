package main;

import java.util.ArrayList;
import java.util.List;

public class TemporalSynchronizer extends Thread {
    private final int id;
    private final int threadCount;
    public DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
    private List<TemporalSynchronizer> threads;
    private volatile boolean running = true;
    private long count = 0;
    private long syncs;

    public TemporalSynchronizer(int id, int threadCount) {

        data.insert(1);
        this.id = id;
        this.threadCount = threadCount;
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        List<TemporalSynchronizer> threads = new ArrayList<>();
        for (int i = 0 ; i < threadCount; i++) {
            TemporalSynchronizer thread = new TemporalSynchronizer(i, threadCount);
            threads.add(thread);
            thread.setPriority(MAX_PRIORITY);
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        long end = System.currentTimeMillis();

        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).join();
        }
        double seconds = (end - start) / 1000.0;
        long totalRequests = 0;
        long totalSynchronizations = 0;
        for (int i = 0 ; i < threadCount; i++) {
            totalRequests += threads.get(i).count;
            totalSynchronizations += threads.get(i).syncs;
        }
        System.out.println(String.format("%d total requests", totalRequests));
        System.out.println(String.format("%d total synchronizations", totalSynchronizations));
        double l = totalRequests / seconds;
        double s = totalSynchronizations / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("%f synchronizations per second", s));
    }

    private void setThreads(List<TemporalSynchronizer> threads) {
        this.threads = threads;
    }

    public void run() {
        while (running) {
            // dresm
            long time = System.nanoTime();
            long timeslotSize = time;
            long seconds = timeslotSize;
            long chunk = ((time / 1_000_000_000));
            long subchunk = 1_000_000_000 / threadCount;

            long start = subchunk * id;
            long endChunk  = subchunk * (id + 1);
//            System.out.println(String.format("%d %d-%d (%d)", id, start, endChunk, chunk));
            if (chunk >= start && chunk < endChunk) {
//                System.out.println("In timeslot");
                syncs++;
                for (TemporalSynchronizer syncer : threads) {
                    count++;
                    syncer.data.insert(syncer.data.tail.value + 1);
                }
//                long end = System.nanoTime();
//                System.out.println(end - time);
            }
        }
    }
}
