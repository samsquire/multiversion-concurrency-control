package main;

import java.util.ArrayList;
import java.util.List;

public class RawAddThroughput extends Thread {
    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        List<RawAddThroughput> threads = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            RawAddThroughput rawAddThroughput = new RawAddThroughput();
            rawAddThroughput.start();
            threads.add(rawAddThroughput);

        }
        long start = System.currentTimeMillis();
        Thread.sleep(10000);
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).running = false;
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).join();
        }
        long end = System.currentTimeMillis();
        float seconds = (end - start) / 1000;
        long total = 0;
        for (int x = 0 ; x < threadCount; x++) {
            total += threads.get(x).number;
        }
        float rps = total/seconds;
        System.out.println(String.format("Additions %d", total));
        System.out.println(String.format("Additions per second %f", rps));
    }
    public volatile boolean running = true;
    public long number;
    public void run() {
        while (running) {
            number++;
        }
    }
}
