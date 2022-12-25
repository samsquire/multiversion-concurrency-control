package main;

import java.util.ArrayList;
import java.util.List;

public class BigIterationBenchmark {
    public static void main(String[] args) {
        int size = 257443412;
        List<Integer> items = new ArrayList<>();
        for (int i = 0 ; i < size; i++) {
            items.add(i);
        }
        System.out.println("Created data");
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0 ; i < size; i++) {
            int current = 100 + items.get(i);
            sum += current;
        }
        long end = System.nanoTime();
        long time = end - start;
        System.out.println(String.format("Iteration time %d ns", time));
        System.out.println(String.format("Time per iteration (add) %d ns", time / size));
        long noAddStart = System.nanoTime();
        sum = 0;
        for (int i = 0 ; i < size; i++) {
            int current = items.get(i);
            sum += current;
        }

        long noAddEnd = System.nanoTime();
        long noAddTime = noAddEnd - noAddStart;

        System.out.println(String.format("Time per iteration (no add) %d ns", noAddTime / size));

    }
}
