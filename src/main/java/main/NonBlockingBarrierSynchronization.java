package main;

import java.util.ArrayList;
import java.util.List;

public class NonBlockingBarrierSynchronization extends Thread {
    private final int id;
    private final List<BarrierTask> tasks;
    private ArrayList<NonBlockingBarrierSynchronization> threads;
    private volatile boolean running = true;

    public NonBlockingBarrierSynchronization(int id) {
        this.id = id;
        tasks = new ArrayList<>();
        for (int x = 0 ; x < 3; x++) {
            BarrierTask barrierTask = new BarrierTask(id, x);
            tasks.add(barrierTask);
        }
        tasks.get(1).wait = true;
        tasks.get(2).wait = true;
    }

    public static void main(String args[]) throws InterruptedException {
        int threadCount = 12;
        long seconds = 5;
        List<NonBlockingBarrierSynchronization> threads = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            NonBlockingBarrierSynchronization thread =
                    new NonBlockingBarrierSynchronization(x);
            threads.add(thread);
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).setThreads(new ArrayList<>(threads));
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).start();
        }
        Thread.sleep(seconds * 1000);
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).running = false;
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).join();
        }

    }
    public void run() {


        while (running) {
            for (int x = 0 ; x < 3; x++) {
                BarrierTask task = tasks.get(x);
                if (x > 0 && task.wait && task.available) {
                    int arrived = 0;
                    int departed = 0;
                    for (NonBlockingBarrierSynchronization thread : threads) {
                        if (thread.tasks.get(x - 1).arrived) {
                            arrived++;
                        }
                        if (thread.tasks.get(x - 1).departed) {
                            departed++;
                        }
                    }
                    if (arrived != threads.size()) {

                        // we cannot continue
                        break;
                    } else {
                        task.available = false;
                        task.run();
                        task.arrive();

                    }
                } else if (task.available) {
                    task.available = false;
                    task.run();
                    task.arrive();
                }
            }
        }
    }
    private void setThreads(ArrayList<NonBlockingBarrierSynchronization> threads) {
        this.threads = threads;
    }

    private static class BarrierTask {
        public boolean departed;
        private volatile boolean wait;
        private volatile boolean arrived;
        private final int id;
        private final int task;
        public volatile boolean available;

        public BarrierTask(int id, int task) {
            this.arrived = false;
            this.id = id;
            this.task = task;
            this.available = true;
            this.wait = false;
            this.departed = false;
        }
        public void run() {
            System.out.println(String.format("Thread %d arrived Task %d", this.id, this.task));
        }

        public void arrive() {
            this.arrived = true;
        }

        public void depart() {
            this.departed = true;
        }
    }
}

