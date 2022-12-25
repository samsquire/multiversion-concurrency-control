package main;

import java.util.ArrayList;
import java.util.List;

public class ShardedTotalOrder extends Thread {
    private final DoublyLinkedList data;
    private final ArrayList<Integer> data2;
    private boolean running = true;
    private long size;
    private int count;
    private int n = 1;
    private int multiple;

    public ShardedTotalOrder(long size, int multiple) {

        this.size = size;
        this.multiple = multiple;
        this.data = new DoublyLinkedList(0, System.currentTimeMillis());
        this.data2 = new ArrayList<>();
    }

    public void run() {
        while (running) {
            n += 1;
            this.data2.add(n);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        long size = Long.MAX_VALUE / threadCount;
        List<ShardedTotalOrder> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            ShardedTotalOrder shardedTotalOrder = new ShardedTotalOrder(size, (i + 1) * threadCount);
            threads.add(shardedTotalOrder);
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
            totalRequests += threads.get(i).data2.size();
        }

        double seconds = (end - start) / 1000.0;



//        int n = 0;
//        int thread = 0;
//        int size1 = threads.size();
//        boolean dataAvailable = true;
//        while (dataAvailable) {
//            n = thread + 1;
//            if (n == size1) {
//                thread = 0;
//                n++;
//            }
//            System.out.println(threads.get(thread).data2.get(n));
//            dataAvailable = n < sizes[thread];
//        }
//        int n = 0;
//        int thread = 0;
//        int size1 = threads.size();
//        boolean dataAvailable = true;
//        while (thread < size1) {
//
//            System.out.println(threads.get(thread).data2.get(n));
//            n = n + 1;
//            if (n == sizes[thread]) {
//                thread++;
//                n = 0;
//            }
//        }
        /* code to iterate through all items in order
        * threads refers to one of the lists */
        int sizes[] = new int[threads.size()];
        for (int i = 0 ; i < threads.size(); i++) {
            sizes[i] = threads.get(i).data2.size();
        }
        int n = 0;
        int thread = 0;
        int size1 = threads.size();
        int offset = 0;
        long iterationStart = System.nanoTime();
        while (thread < size1) {

            // System.out.println(String.format("%d %d", thread, offset + threads.get(thread).data2.get(n)));
            int current = offset + threads.get(thread).data2.get(n);
            n = n + 1;
            if (n == sizes[thread]) {
                offset += sizes[thread];
                thread++;
                n = 0;
            }
        }
        long iterationEnd = System.nanoTime();
        long iterationTime = iterationEnd - iterationStart;
        long numberOfItems = 0;
        for (Integer amount : sizes) {
            numberOfItems += amount;
        }
        System.out.println(String.format("Iteration time %d ns", iterationTime));
        System.out.println(String.format("Number of items %d", numberOfItems));
        System.out.println(String.format("Iteration cost %d", iterationTime / numberOfItems ));
        System.out.println("Lookup");
        long lookupStart = System.nanoTime();

        int lookupKey = 329131;
        int current = lookupKey;
        int currentThread = 0;
        int total = 0;
        while (current >= 0 && currentThread <= size1 - 1) {
            int next = current - sizes[currentThread];

            if (next >= 0) {
                total += sizes[currentThread];
                current -= sizes[currentThread];
                currentThread++;

            } else {
                break;
            }

        }
        long lookupEnd = System.nanoTime();
        long lookupTime = lookupEnd - lookupStart;
        System.out.println(String.format("%d %d",
                currentThread,
                total + threads.get(currentThread).data2.get(current)));
        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
        System.out.println(String.format("Lookup time %dns", lookupTime));

    }
}
