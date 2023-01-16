package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ShardedHashMap extends Thread {
    private final int id;
    private List<ShardedHashMap> threads;
    private volatile boolean running = true;

    private AtomicInteger counter;

    private Map<Integer, Integer> data;

    private volatile int[] requests;
    private volatile int[] values;
    private int n;


    public static void main(String args[]) throws InterruptedException {
        int threadCount = 11;
        List<ShardedHashMap> threads = new ArrayList<>();
        for (int i = 0 ; i < threadCount ; i++) {
            Map<Integer, Integer> data = new HashMap<>();
            for (int j = 0 ; j < threadCount; j++) {
                data.put(j, j);
            }
            ShardedHashMap thread = new ShardedHashMap(i, data);
            threads.add(thread);
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).start();
        }
        Thread.sleep(5000);
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).join();
        }
        int total = 0;
        for (ShardedHashMap thread : threads) {
            total += thread.n;
        }
        System.out.println(String.format("Total requests %d", total));
        System.out.println(String.format("Total requests per second %d", (total / 5)));
    }
    public ShardedHashMap(int id, Map<Integer, Integer> data) {
        this.id = id;
        this.data = data;
        this.counter = new AtomicInteger(0);
    }
    private void setThreads(ArrayList<ShardedHashMap> threads) {
        this.threads = threads;
        this.requests = new int[threads.size()];
        this.values = new int[threads.size()];
    }

    public void run() {
        int expectedValue = id * threads.size();
        boolean requested = false;
        while (running) {
            if (counter.get() == threads.size()) {
                counter.set(0);
                requested = false;
                n++;
            }
            // process other threads
            for (int x = 0 ; x < requests.length; x++) {
                if (requests[x] != -1) {
                    values[x] = data.get(requests[x]);
                    requests[x] = -1;
                    threads.get(x).counter.incrementAndGet();
                }

            }

            if (!requested) {
                requested = true;
                for (ShardedHashMap thread : threads) {
                    thread.requests[id] = id;
                }
            }
        }
    }
}
