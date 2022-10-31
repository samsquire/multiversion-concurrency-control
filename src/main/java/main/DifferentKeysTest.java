package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DifferentKeysTest extends Thread {

    private static int id;
    private Map<Integer, Integer> data;
    private volatile boolean running;

    public DifferentKeysTest(int id, Map<Integer, Integer> data) {
        this.id = id;
        this.data = data;
    }

    public static void main(String[] args) throws InterruptedException {
        Map<Integer, Integer> data = new HashMap<>();
        int threadCount = 100;
        List<DifferentKeysTest> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            DifferentKeysTest dkt = new DifferentKeysTest(id, data);
            threads.add(dkt);
            id += 100;
        }
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).start();
        }
        Thread.sleep(5000);
        for (int i = 0 ; i < threadCount ; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).join();
        }
        System.out.println("Finished");
    }

    public void run() {
        while (running) {
            for (int i = 0 ; i < 100; i++) {
                if (data.containsKey(i)) {
                    data.put(id + i, data.get(id + i) + 1);
                } else {
                    data.put(id + i, 0);
                }

            }
        }
    }
}
