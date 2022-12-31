package main;

import java.util.ArrayList;
import java.util.List;

public class TokenRingTimer extends Thread {

    private int id;
    private DoublyLinkedList data;
    private ArrayList<TokenRingTimer> threads;
    private boolean running = true;
    private volatile boolean clear = false;
    private volatile boolean finishedReading = false;
    private boolean reading = false;

    public TokenRingTimer(int id, DoublyLinkedList data) {
        this.id = id;
        this.data = data;
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        List<TokenRingTimer> threads = new ArrayList<>();
        data.insert(1);
        for (int i = 0; i < threadCount; i++) {
            TokenRingTimer thread = new TokenRingTimer(i, data);
            thread.reading = true;
            threads.add(thread);
        }
        threads.get(0).clear = true;
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(5000);
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).join();
        }
        long end = System.currentTimeMillis();

        double seconds = (end - start) / 1000.0;
        System.out.println(String.format("Total Requests %d", data.tail.value));
        System.out.println(String.format("Time elapsed %f", seconds));
        System.out.println(String.format("Requests per second %f",
                data.tail.value / seconds));
    }

    private void setThreads(ArrayList<TokenRingTimer> tokenRingTimers) {
        this.threads = tokenRingTimers;
    }

    public void run() {
        int lastValue = 0;
        while (running) {

            if (clear) {
                // System.out.println("Writing");
                data.insert(data.tail.value + 1);
                clear = false;

                threads.get((id + 1) % threads.size()).clear = true;
            }
        }
    }
}
