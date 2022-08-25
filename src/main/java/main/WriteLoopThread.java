package main;


import java.util.LinkedList;
import java.util.List;

public class WriteLoopThread extends Thread {

    private final List<Loop> loops;
    private LinkedList<Integer> list;
    public int cycles;

    WriteLoopThread(List<Loop> loops, LinkedList<Integer> list) {
        this.loops = loops;
        this.list = list;
    }

    private boolean stopped;

    public void finished() {
        stopped = true;
    }

    public void run() {
        boolean allempty = false;
        while (!stopped || !allempty) {

            // System.out.println("read mode");
            for (Loop loop : loops) {
                loop.markReading();
            }

            int count = loops.size();
            while (count > 0) {
                count = loops.size();

                for (Loop loop : loops) {
                    if (loop.finished || !loop.isStillReading()) {
                        count--;
                    }
                }
                // System.out.println(String.format("Waiting for all thread to read... %d", count));
            }
            for (Loop loop : loops) {
                // wait for reading to synchronize
                while (!loop.finished && !loop.blockingReading) {
                    // System.out.println("Waiting for thread to read...");
                }
            }

            for (Loop loop : loops) {
                loop.unblockReading();
            }

            // System.out.println("write mode");
            allempty = true;
            boolean allwrote = false;
            while (!allwrote) {
                allwrote = true;
                for (Loop loop : loops) {

                    loop.block();
                    while (!loop.finished && !loop.blocking) {
                        // System.out.println("Waiting for thread to block...");
                    }
                    if (loop.changes.size() > 0) {
                        allempty = false;
                    }
                    if (loop.finished || loop.blocking) {
                        // System.out.println("Doing work");
                        List<Change> changes = loop.exchangeChanges();
                        for (Change change : changes) {
                            change.apply();
                        }
                        loop.unblock();
                    } else {
                        allwrote = false;
                    }

                }
            }
            cycles++;
        }
    }
}
