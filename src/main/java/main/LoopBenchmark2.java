package main;

public class LoopBenchmark2 {
    public static void main(String args[]) {
        int y = 0;
        long start = System.currentTimeMillis();
        for (int x = 0 ; x < 1000000000 ; x++) {
            y = y + 1;
        }
        long end = System.currentTimeMillis();
        System.out.println(String.format("Time: %d", end - start));
    }
}
