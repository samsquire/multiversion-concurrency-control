package main;

import java.util.*;

public class ShardedTotalRandomOrder extends Thread {
    private final DoublyLinkedList data;
    private final ArrayList<Tuple> data2;
    private boolean running = true;
    private long size;
    private int count;
    private int n = 1;
    private int multiple;
    private int id;

    public ShardedTotalRandomOrder(int id, long size, int multiple) {
        this.id = id;
        this.size = size;
        this.multiple = multiple;
        this.data = new DoublyLinkedList(0, System.currentTimeMillis());
        this.data2 = new ArrayList<Tuple>();
    }

    public void run() {
        Random rng = new Random();
        while (running) {
            n += 1;
            this.data2.add(new Tuple(id, rng.nextInt()));
        }
        this.data2.sort(new Comparator<Tuple>() {
            @Override
            public int compare(Tuple o1, Tuple o2) {
                return o1.value.compareTo(o2.value);
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        long size = Long.MAX_VALUE / threadCount;
        List<ShardedTotalRandomOrder> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            ShardedTotalRandomOrder shardedTotalOrder = new ShardedTotalRandomOrder(i, size, (i + 1) * threadCount);
            threads.add(shardedTotalOrder);
        }
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(50);
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).join();
        }
        System.out.println("Finished");
        long end = System.currentTimeMillis();

        long totalRequests = 0;

        for (int i = 0; i < threads.size(); i++) {
            totalRequests += threads.get(i).data2.size();
        }

        double seconds = (end - start) / 1000.0;


        int sizes[] = new int[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            sizes[i] = threads.get(i).data2.size();
        }
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
//        int n = 0;
//        int thread = 0;
//        int size1 = threads.size();
//        boolean dataAvailable = true;
//        int offset = 0;
//        long iterationStart = System.nanoTime();
//        while (thread < size1) {
//
//            // System.out.println(String.format("%d %d", thread, offset + threads.get(thread).data2.get(n)));
//            int current = offset + threads.get(thread).data2.get(n);
//            n = n + 1;
//            if (n == sizes[thread]) {
//                offset += sizes[thread];
//                thread++;
//                n = 0;
//            }
//        }
//        long iterationEnd = System.nanoTime();
//        long iterationTime = iterationEnd - iterationStart;
//        long numberOfItems = 0;
//        for (Integer amount : sizes) {
//            numberOfItems += amount;
//        }
//        System.out.println(String.format("Iteration time %d ns", iterationTime));
//        System.out.println(String.format("Number of items %d", numberOfItems));
//        System.out.println(String.format("Iteration cost %d", iterationTime / numberOfItems ));

        int size1 = threads.size();

        int[] positions = new int[size1];
        Arrays.fill(positions, 0);
        PriorityQueue<Tuple> pq = new PriorityQueue<>(new Comparator<Tuple>() {
            @Override
            public int compare(Tuple o1, Tuple o2) {
                return o1.value.compareTo(o2.value);
            }
        });
        long startOrderedIteration = System.nanoTime();

        for (ShardedTotalRandomOrder thread : threads) {
            for (int i = 0; i < 10; i++) {
//                System.out.println(thread.data2.get(i));
                pq.add(thread.data2.get(i));
            }
        }
        List<Integer> overall = new ArrayList<>();
        while (!pq.isEmpty()) {
            Tuple poll = pq.poll();
            ArrayList<Tuple> data2 = threads.get(poll.thread).data2;
            if (positions[poll.thread] < data2.size()) {
                Tuple nextValue = data2.get(positions[poll.thread]++);
                pq.offer(nextValue);
            }
            overall.add(poll.value);
            // System.out.println(String.format("%d %d", poll.thread, poll.value));
        }
        // System.out.println(overall);
        long endOrderedIteration = System.nanoTime();
        long orderedIterationTime = endOrderedIteration - startOrderedIteration;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
        System.out.println(String.format("Ordered iteration time taken: %d", orderedIterationTime));
        System.out.println(String.format("Ordered iteration time taken: %d", orderedIterationTime / 1000000000));
        long lookupStart = System.nanoTime();

        int lookupKey = 329131;
        System.out.println(String.format("Lookup index %d", lookupKey));
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
        System.out.println(String.format("Partial order Lookup time %d", lookupTime));
        Integer overallValue = overall.get(lookupKey);
        System.out.println(String.format("Overall sorted index item %d",
                overallValue));
        pq.clear();
        long lookupStart2 = System.nanoTime();

        for (ShardedTotalRandomOrder thread : threads) {
            for (int i = 0; i < 10; i++) {
//                System.out.println(thread.data2.get(i));
                pq.add(thread.data2.get(i));
            }
        }
        int n = 0;
        int target = lookupKey;
        Tuple poll = null;
        Arrays.fill(positions, 0);

        while (n <= target) {
            poll = pq.poll();
            ArrayList<Tuple> data2 = threads.get(poll.thread).data2;
            if (positions[poll.thread] < data2.size()) {
                Tuple nextValue = data2.get(positions[poll.thread]++);
                pq.add(nextValue);
            }
            n++;
            // System.out.println(String.format("%d %d", poll.thread, poll.value));
        }
        System.out.println(String.format("Lookup by calculation %d %d",
                currentThread,
                poll.value));
        long lookupEnd2 = System.nanoTime();
        long lookupTime2 = lookupEnd2 - lookupStart2;
        System.out.println(String.format("Accurate Lookup time %d", lookupTime2));
    }

    static class Tuple {
        private final int thread;
        private final Integer value;

        public Tuple(int thread, Integer value) {
            this.thread = thread;
            this.value = value;
        }
    }
}
