package main;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class ConcurrentLoopThread extends Thread {
    private final int start;
    private final int size;
    private final int limit;
    private List<ConcurrentLoop> tickers;
    private int threadNum;
    public List<Integer> currents = new ArrayList<>();
    public List<Integer> end = new ArrayList<>();
    private List<Boolean> finished = new ArrayList<>();
    private int current;
    int n;

    public ConcurrentLoopThread(List<ConcurrentLoop> tickers, int start, int size, int threadNum) {
        this.tickers = tickers;
        this.threadNum = threadNum;

        this.n = start;
        this.start = start;
        this.size = size;
        this.limit = start + size;
        for (int i = 0; i < tickers.size(); i++) {
            currents.add(i, start);
            end.add(i, Math.min(start + tickers.get(i).size() - 1, limit));
            finished.add(i, false);
        }
        System.out.println(String.format("Start %d/%d", start, limit));
    }

    public void run() {
        super.run();
        boolean running = true;
        // System.out.println(String.format("Activate %s", start));
        current = 0;
        while (running) {
            running = false;
            for (int i = 0; i < tickers.size(); i++) {
                end.add(i, Math.min(tickers.get(i).size() - 1, limit));
                if (i >= currents.size() || i >= end.size()) { // learn of new ticker
                    for (int j = currents.size(); j <= i; j++) {
                        currents.add(j, start);
                    }
                    for (int j = end.size(); j <= i; j++) {
                        end.add(j, Math.min(tickers.get(j).size() - 1, limit));
                    }
                    for (int j = finished.size(); j <= i; j++) {
                        finished.add(j, false);
                    }

                }
                if (!finished.get(i)) {
                    running = true;
                }
                else if (currents.get(i) < end.get(i)) {
                    running = true; // a ticker is unfinished
                } else if (currents.get(i).equals(end.get(i))) { // ticker is finished, reload it
                    // System.out.println("Finsihed ticker");
                    // tickers.get(i).reload();
                    // end.set(i, Math.min(tickers.get(i).size(), start + size));
                    // currents.set(i, start);
                }
            }



            // System.out.println(String.format("Thread %d Currents: %d Ends: %d", threadNum, currents.get(current), end.get(current)));
            if (!finished.get(current) && currents.get(current) >= start && (currents.get(current) < end.get(current)) && currents.get(current) < limit) {
                StringOrConcurrentLoop scl = tickers.get(current).tick(currents.get(current));
                tickers.get(current).addThread(threadNum);
                // System.out.println(String.format("Ticking %d", current));
                if (scl == null) {
                } else if (scl.isString()) {
                    System.out.println(String.format("THREAD %s", scl.value));
                } else if (scl.isLoops()) {
                    for (int i = 0; i < scl.loops.size(); i++) {


                        currents.add(start);

                        end.add(Math.min(scl.loops.get(i).size() - 1, limit));
                        tickers.add(scl.loops.get(i));
                        finished.add(false);
                    }
                }

            }

            int previous = currents.get(current);


            if (previous >= limit) {

                finished.set(current, true);
                currents.set(current, (((currents.get(current) + 1))));
            } else {
                currents.set(current, (((currents.get(current) + 1))));
            }

            current = (current + 1) % currents.size();
        }

    }
}
