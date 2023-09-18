package main;

import java.util.ArrayList;
import java.util.List;

public class ArrayListBenchmark extends Thread {
    private long n;
    private List<Long> data= new ArrayList<>();
    private volatile boolean running = true;

    public ArrayListBenchmark() {

    }
    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        int seconds = 12;
        ArrayList<ArrayListBenchmark> threads = new ArrayList();
        for (int x = 0 ; x < threadCount ; x++) {
            threads.add(new ArrayListBenchmark());
        }
        for (int x = 0 ; x < threadCount ; x++) {
            threads.get(x).start();
        }
        Thread.sleep(seconds * 1000);
        for (int x = 0 ; x < threadCount ; x++) {
            threads.get(x).running = false;
        }
        for (int x = 0 ; x < threadCount ; x++) {
            threads.get(x).join();
        }
        long size = 0;
        for (int x = 0 ; x < threadCount ; x++) {
            size += threads.get(x).data.size();
        }
        System.out.println(String.format("Requests %d", size));
        System.out.println(String.format("Requests per second %d", size / seconds));
    }

    public void run() {
        while (running) {
            data.add(System.currentTimeMillis());
        }
    }
}
