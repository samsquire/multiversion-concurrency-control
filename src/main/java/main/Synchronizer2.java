package main;

import java.util.ArrayList;
import java.util.List;

public class Synchronizer2 extends Thread {
    int threadCount;
    private boolean running = true;
    int id;
    DoublyLinkedList data;
    private Integer n;
    private long timestamp;

    public Synchronizer2(int size, int id, int n) {
        this.data = new DoublyLinkedList(id, System.currentTimeMillis());
        this.n = 0;
    }
    public void setThreads(List<Synchronizer2> threads) {
        this.threadCount = threads.size();
    }
    public void run() {
        while (running) {
            this.data.insert(this.n * id);
            this.n = n + 1;
        }
    }

    public static void main(String args[]) throws InterruptedException {
        List<Synchronizer2> threads = new ArrayList<>();
        int size = 11;
        for (int i = 0 ; i < size; i++) {
            threads.add(new Synchronizer2(size, i, 0));

        }
        for (int i = 0 ; i < size; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }

        for (int i = 0 ; i < size; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(5000);
        for (int i = 0 ; i < size; i++) {
            threads.get(i).running = false;
        }
        System.out.println("Finished");
        long end = System.currentTimeMillis();
        ArrayList<Integer> forwardIteration = new ArrayList<>();
        System.out.println("Forward iteration");
        DoublyLinkedList data[] = new DoublyLinkedList[threads.size()];
        long timestamps[] = new long[threads.size()];
        for (int i = 0 ; i < size ; i++) {
            data[i] = threads.get(i).data;
            timestamps[i] = threads.get(i).timestamp;
        }
        boolean allIterated = false;
        while (!allIterated) {
            allIterated = true;
            long min = Long.MAX_VALUE;
            int value = -1;
            for (int i = 0 ; i < size; i++) {
                if (timestamps[i] < min) {
                    min = timestamps[i];
                    value = i;
                }
            }
            forwardIteration.add(data[value].value);
            data[value] = data[value].tail;
            if (data[value] != null) {
                allIterated = false;
            }
        }
        int totalRequests = 0;
        for (int i = 0 ; i < size; i++) {
            totalRequests += threads.get(i).n;
        }
        double seconds = (end - start) / 1000.0;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }
}
