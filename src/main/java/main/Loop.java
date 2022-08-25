package main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Loop extends Thread {

    private final LinkedList<Integer> list;
    public volatile boolean blocking = false;
    public boolean finished;
    private int key;
    public List<Change> changes;
    public int size;
    private volatile boolean blocked;
    private boolean reading;
    private boolean doneRead;
    private volatile boolean blockedReading;
    public volatile boolean blockingReading;

    public Loop(LinkedList<Integer> list, int key) {
        this.list = list;
        this.key = key;
        this.changes = new ArrayList<>();
    }

    public boolean stopped;

    public void finished() {
        stopped = true;
    }


    public void run() {
        System.out.println(String.format("Thread %d started", key));
        while (!stopped) {

            if (reading) {
                // System.out.println(String.format("%d Do reading", key));

                try {
                    Thread.sleep(100);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // System.out.println("Thread finished reading");
                doneRead = true;
                while (blockedReading) {
                    // System.out.println("Waiting for other readers to finish");
                    blockingReading = true;
                }
                reading = false;
                blockingReading = false;
                doneRead = false;
            }


            while (blocked) {
                // System.out.println("Waiting");
                blocking = true;
            }
            // System.out.println("Running");
            changes.add(new Change(new Runnable() {
                @Override
                public void run() {
                    list.addFirst(key);
                }
            }));
            changes.add(new Change(new Runnable() {
                @Override
                public void run() {
                    list.addLast(key);
                }
            }));
            size += 2;


        }
        finished = true;
        blocking = true;
    }

    public List<Change> exchangeChanges() {
        List<Change> change = changes;
        this.changes = new ArrayList<>();

        return change;
    }

    public void block() {
        this.blocked = true;
    }

    public void unblock() {
        this.blocked = false;
        this.blocking = false;
    }

    public void markReading() {
        blockedReading = true;
        reading = true;
    }

    public boolean isStillReading() {
        return !doneRead;
    }

    public void blockReading() {
        this.blockedReading = true;
    }

    public void unblockReading() {
        blockedReading = false;
    }
}
