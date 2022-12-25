package main;

import java.util.ArrayList;
import java.util.List;

public class NoAddBenchmark extends Thread {
    private boolean running = true;
    private int size;
    private List<Integer> data;
    private int n;

    public NoAddBenchmark(int size) {
        this.size = size;
        data = new ArrayList<>();
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        int size = 1000000;
        List<NoAddBenchmark> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            NoAddBenchmark thread = new NoAddBenchmark(i * size);
            threads.add(thread);
        }
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).running = false;
        }
        System.out.println("Finished");
        long end = System.currentTimeMillis();

        long totalRequests = 0;

        for (int i = 0; i < threads.size(); i++) {
            totalRequests += threads.get(i).data.size();
        }

        double seconds = (end - start) / 1000.0;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));

    }
    public void run() {

        while (running) {
            this.data.add(n++);
        }
    }
}
