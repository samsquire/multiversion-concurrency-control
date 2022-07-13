package main;

import java.util.ArrayList;
import java.util.List;

public class ConcurrentLoopThread extends Thread {
    private final int start;
    private final int size;
    private final int limit;
    private List<ConcurrentLoop> tickers;
    private int threadNum;
    private List<Integer> currents = new ArrayList<>();
    private List<Integer> end = new ArrayList<>();
    private int current;
    int n;

    public ConcurrentLoopThread(List<ConcurrentLoop> tickers, int start, int size, int threadNum) {
        this.tickers = tickers;
        this.threadNum = threadNum;
        for (int i = 0; i < tickers.size(); i++) {
            currents.add(i, start);
            end.add(i, Math.min(tickers.get(i).size(), start + size));
        }
        this.n = start;
        this.start = start;
        this.size = size;
        this.limit = start + size;
    }

    public void run() {
        super.run();
        boolean running = true;
        System.out.println(String.format("Activate %s", start));
        current = 0;
        while (running) {
            running = false;
            for (int i = 0; i < tickers.size(); i++) {
                if (i >= currents.size() || i >= end.size()) { // learn of new ticker
                    for (int j = currents.size(); j <= i; j++) {
                        currents.add(start);
                    }
                    for (int j = end.size(); j <= i; j++) {
                        end.add(Math.min(start + tickers.get(j).size() - 1, start + size - 1));
                    }

                }
                if (currents.get(i) < end.get(i)) {
                    running = true; // a ticker is unfinished
                } else if (currents.get(i).equals(end.get(i))) { // ticker is finished, reload it
                    // System.out.println("Finsihed ticker");
                    // tickers.get(i).reload();
                    // end.set(i, Math.min(tickers.get(i).size(), start + size));
                    // currents.set(i, start);
                }
            }
//                if (tickers.get(current).getClass().equals(ConcurrentLoopJoin.class)) {
//                    System.out.println(tickers.get(current));
//                }

            // System.out.println(String.format("Thread %d Currents: %d Ends: %d", threadNum, currents.get(current), end.get(current)));
            if (currents.get(current) >= start && (currents.get(current) < end.get(current))) {
                StringOrConcurrentLoop scl = tickers.get(current).tick(currents.get(current));

                if (scl == null) {
                } else if (scl.isString()) {
                    System.out.println(String.format("THREAD %d: %s", threadNum, scl.value));
                } else if (scl.isLoops()) {
                    for (int i = 0; i < scl.loops.size(); i++) {


                        currents.add(start);
                        end.add(Math.min(start + scl.loops.get(i).size() - 1, start + size - 1));
                        tickers.add(scl.loops.get(i));
                    }
                }

            }
            currents.set(current, currents.get(current) + 1);
            current = (current + 1) % currents.size();
        }

    }
}
