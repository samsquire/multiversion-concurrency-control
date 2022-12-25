package main;

import java.util.ArrayList;
import java.util.List;

public class DoublyLinkedListBenchmark extends Thread {
    private static boolean running = true;
    private DoublyLinkedList list;
    private int counter = 0;
    public DoublyLinkedListBenchmark(DoublyLinkedList list) {
        this.list = list;
    }

    public void run() {

        while (running) {
            list.insert(counter++);
        }
    }

    public static void main(String[] argv) throws InterruptedException {
        DoublyLinkedList list = new DoublyLinkedList(0, System.currentTimeMillis());
        DoublyLinkedListBenchmark thread = new DoublyLinkedListBenchmark(list);
        long start = System.currentTimeMillis();
        thread.start();
        Thread.sleep(5000);
        thread.running = false;
        thread.join();
        long end = System.currentTimeMillis();

        long totalRequests = thread.counter;



        double seconds = (end - start) / 1000.0;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));

        DoublyLinkedList current = list;
//        List<Integer> numbers = new ArrayList<>();
//        while (current != null) {
//            numbers.add(current.value);
//            current = current.tail;
//        }
//        System.out.println(numbers);

    }
}
