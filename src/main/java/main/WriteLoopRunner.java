package main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class WriteLoopRunner {
    public static void main(String[] args) throws InterruptedException {
        LinkedList<Integer> linkedList = new LinkedList<Integer>();
        List<Loop> threads = new ArrayList<>();
        for (int i = 0 ; i < 100; i++) {
            Loop loop = new Loop(linkedList, i);
            threads.add(loop);
        }
        WriteLoopThread writeLoop = new WriteLoopThread(threads, linkedList);
        for (Loop loop : threads) {
            loop.start();
        }
        writeLoop.start();
        Thread.sleep(1000);
        for (Loop loop : threads) {
            loop.finished();
        }
        Thread.sleep(1000);

        for (Loop loop : threads) {
            loop.join();
        }
        writeLoop.finished();
        writeLoop.join();
        System.out.println("Finished, now checking...");
        for (int i = 0 ; i < 100; i++) {
            int expectedSize = threads.get(i).size;
            int count = 0;
            for (Integer j : linkedList) {
                if (j.equals(i)) {
                    count++;
                }
            }
            assert expectedSize == count : String.format("%d != %d", expectedSize, count);
        }
        System.out.println(String.format("Number of cycles: %d", writeLoop.cycles));
    }
}
