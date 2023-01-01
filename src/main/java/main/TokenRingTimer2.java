package main;

import java.util.ArrayList;
import java.util.List;

public class TokenRingTimer2 extends Thread {

    private int id;
    private DoublyLinkedList data;
    private ArrayList<TokenRingTimer2> threads;
    private boolean running = true;
    private volatile boolean clear = false;
    private volatile boolean wakeup = false;
    private volatile boolean acknowledge = false;
    private volatile boolean finishedReading = false;
    private volatile boolean reading = false;
    private int writingCount;
    private int readingCount;
    private boolean finishedWrite;
    private boolean allFinished = true;
    private boolean writeReset;
    private boolean readingStopped;
    private boolean readingCancelled;

    public TokenRingTimer2(int id, DoublyLinkedList data) {
        this.id = id;
        this.data = data;
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        List<TokenRingTimer2> threads = new ArrayList<>();
        data.insert(1);
        for (int i = 0; i < threadCount; i++) {
            TokenRingTimer2 thread = new TokenRingTimer2(i, data);
            thread.reading = true;
            thread.finishedReading = true;
            thread.allFinished = true;
            threads.add(thread);
        }
        threads.get(0).writeReset = true;
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

        List<Integer> items = new ArrayList<>();
        DoublyLinkedList current = data;
        while (current != null) {
            items.add(current.value);
            current = current.tail;
        }
        int readings = 0;
        int writings = 0;
        for (int x = 0; x < threads.size(); x++) {
            readings += threads.get(x).readingCount;
            writings += threads.get(x).writingCount;
        }

        double seconds = (end - start) / 1000.0;
//        System.out.println(String.format("Total Requests %d", items.size()));
//        System.out.println(String.format("Time elapsed %f", seconds));
//        System.out.println(String.format("Readings %d", readings));
//        System.out.println(String.format("Writings %d", writings));
        int sumRW = readings + writings;
//        System.out.println(String.format("Total reading+writings %d", sumRW));
        System.out.println(String.format("Total writing %d", writings));
        System.out.println(String.format("Total reading+writings per second %f", sumRW / seconds));
        System.out.println(String.format("Requests per second %f",
                items.size() / seconds));
    }

    private void setThreads(ArrayList<TokenRingTimer2> tokenRingTimers) {
        this.threads = tokenRingTimers;
    }

    public void run() {
        int lastValue = 0;
        int next = id + 1;
        int last = threads.size() - 1;

        while (running) {

            boolean allFinished = true;

            if (reading) {
                readingCancelled = false;
                readingCount++;
                data.read();
                lastValue = data.tail.value + id;
                finishedReading = true;
            } else {
                readingCancelled = true;
            }
            if (id == 0 && writeReset) {
                if (finishedReading) {
                    for (TokenRingTimer2 thread : threads) {
                        if (!thread.finishedReading) {
                            allFinished = false;
                            // System.out.println(String.format("%d not finished reading",
                                    // thread.id));
                            break;
                        }
                    }
                    if (allFinished) {
                        // System.out.println(String.format("Finished reading %s", finishedReading));

                        // System.out.println("Stopping threads");
                        for (TokenRingTimer2 thread : threads) {
                            thread.reading = false;
                        }
                        boolean allCancelled = true;
                        for (TokenRingTimer2 thread : threads) {
                            if (thread != this) {
                                if (!thread.readingCancelled) {
                                    allCancelled = false;
                                    // System.out.println(String.format("%d is not cancelled", thread.id));
                                }
                            }
                        }
                        // System.out.println(String.format("State %s %s %s", allFinished, allCancelled, writeReset));
                        if (allFinished && allCancelled && writeReset) {
                            clear = true;
                            writeReset = false;
                            // System.out.println("New cycle");
                        }
                    }
                }
            }

            if (clear) {
                writingCount++;
                // System.out.println(String.format("%d Writing", id));
                data.insert(lastValue);
                clear = false;


                if (next == last) {
                    for (TokenRingTimer2 thread : threads) {
                        thread.readingCancelled = false;
                        thread.finishedReading = false;
                        thread.reading = true;
                    }
                    threads.get(0).writeReset = true;
                    // System.out.println("0 turn to write");
                } else {
                    TokenRingTimer2 nextThread = threads.get((next) % threads.size());
                    nextThread.clear = true;
                    // System.out.println(String.format("Passing the baton to %d", next));

                }
            }

        }
    }
}
