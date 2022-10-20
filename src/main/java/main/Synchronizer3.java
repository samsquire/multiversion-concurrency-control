package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Synchronizer3 extends Thread {
    private long data;
    private Synchronizer3 main;
    private int id;
    public boolean synchronizer;
    public volatile boolean running = true;
    private Comparator<Integer> comparator;
    private List<Synchronizer3> threads;
    private int threadsSize;
    private volatile boolean dirty = false;
    private Integer n = 0;

    public Synchronizer3(int threadCount, Synchronizer3 main, int id, boolean synchronizer, Comparator<Integer> comparator) {
        this.main = main;
        this.id = id;
        this.synchronizer = synchronizer;
        this.comparator = comparator;

    }

    public void setThreads(List<Synchronizer3> threads) {
        this.threads = threads;
        this.threadsSize = threads.size();
    }

    public static void main(String args[]) throws InterruptedException {
        int size = 12;
        int id = 0;
        List<Synchronizer3> threads = new ArrayList<>();
        Comparator<Integer> comp = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return (Integer) o1 - (Integer) o2;
            }
        };
        Synchronizer3 syncThread = new Synchronizer3(size, null, id++, true, comp);
        threads.add(syncThread);
        for (int i = 0; i < size - 1; i++) {
            Synchronizer3 synchronizer3 = new Synchronizer3(size, syncThread, id++, false, comp);
            threads.add(synchronizer3);
        }
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }
        for (Synchronizer3 thread : threads) {
            thread.start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(5000);
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).running = false;
        }
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
        }
        System.out.println("Finished");
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

    public void run() {

        if (!synchronizer) {
            while (running) {
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
            }
        }
    }

}
