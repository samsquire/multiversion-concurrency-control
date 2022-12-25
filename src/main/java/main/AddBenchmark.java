package main;

import java.util.ArrayList;
import java.util.List;

public class AddBenchmark extends Thread {
    private boolean running = true;
    private int size;
    private List<Integer> data;
    private int n;
    private List<Integer> numbers;

    public AddBenchmark(int size, List<Integer> numbers) {
        this.size = size;
        this.numbers = numbers;
        data = new ArrayList<>();
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        int size = 1000000;
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0 ; i < size; i++) {
            numbers.add(i);
        }
        List<AddBenchmark> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            AddBenchmark thread = new AddBenchmark(i * size, numbers);
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
            for (int i = 0; i < numbers.size(); i++) {
                this.data.add(size + numbers.get(i));
            }
        }
    }
}
