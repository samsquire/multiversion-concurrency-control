package main;

import java.util.ArrayList;
import java.util.List;

public class EpochThreadScheduler extends Thread {


    private DoublyLinkedList data;
    private long total;
    private int lastEpoch = -1;

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 23;
        List<List<Integer>> epochs = new ArrayList<>();
        List<Integer> incrementers = new ArrayList<>();
        List<Integer> empty = new ArrayList<>();
        incrementers.add(0);
        List<Integer> summers = new ArrayList<>();
        summers.add(2);

        epochs.add(incrementers);
        epochs.add(empty);
        epochs.add(summers);
        epochs.add(empty);
        List<EpochThreadScheduler> threads = new ArrayList<>();
        DoublyLinkedList data = new DoublyLinkedList(0, 0);
        data.insert(0);
        EpochThreadScheduler scheduler = new EpochThreadScheduler("scheduler", data,0, incrementers, epochs);

        for (int x = 0 ; x < threadCount - 1 ; x++) {
            EpochThreadScheduler worker = new EpochThreadScheduler("incrementer", data, x, incrementers, epochs);
            threads.add(worker);
        }
        EpochThreadScheduler worker = new EpochThreadScheduler("summer", data, threadCount - 1, summers, epochs);
        threads.add(worker);

        worker.setThreads(new ArrayList<>(threads));
        scheduler.setThreads(new ArrayList<>(threads));

        for (int x = 0 ; x < threadCount ; x++) {
            threads.get(x).start();
        }
        scheduler.start();
        Thread.sleep(10000);
        scheduler.running = false;
        scheduler.join();
        for (int x = 0 ; x < threadCount ; x++) {
            threads.get(x).running = false;
        }
        for (int x = 0 ; x < threadCount ; x++) {
            threads.get(x).join();
        }
        System.out.println(String.format("Sum %d", worker.total));

    }
    volatile int epoch;
    private List<EpochThreadScheduler> threads;
    private String mode;
    private int id;
    private List<Integer> myEpochs;
    private List<List<Integer>> epochs;
    private boolean running = true;
    private boolean finished;


    private long sum;
    public EpochThreadScheduler(String mode,
                                DoublyLinkedList data,
                                int id,
                                List<Integer> myEpochs,
                                List<List<Integer>> epochs) {
        this.mode = mode;
        this.data = data;
        this.id = id;
        this.myEpochs = myEpochs;
        this.epochs = epochs;
    }
    private void setThreads(List<EpochThreadScheduler> threads) {
        this.threads = threads;
    }

    public void run() {
        if (mode.equals("scheduler")) {
            while (running) {
                lastEpoch = epoch;
                int nextEpoch = (epoch + 1) % epochs.size();
                boolean allFinished = true;
                for (EpochThreadScheduler thread : threads) {
                    if (thread.myEpochs.contains(lastEpoch)) {
                        // System.out.println(String.format("%d %d", thread.lastEpoch, epoch));
                        if (thread.lastEpoch != lastEpoch) {
                            allFinished = false;
                            break;
                        }
                    }
                }
                if (allFinished) {
                    for (EpochThreadScheduler thread : threads) {
                        thread.epoch = nextEpoch;
                    }
                    epoch = nextEpoch;
                }
            }
        }

        if (mode.equals("incrementer")) {
            while (running) {

                if (myEpochs.contains(epoch)) {

                    sum++;
                    lastEpoch = epoch;

                } else {
                    lastEpoch = epoch;
                }
            }
        }
        if (mode.equals("summer")) {
            while (running) {
                if (myEpochs.contains(epoch)) {
                    long total = 0;
                    for (EpochThreadScheduler thread : threads) {
                        total += thread.sum;
                    }
                    this.total = total;
                    lastEpoch = epoch;
                } else {
                    lastEpoch = epoch;

                }
            }
        }
    }
}
