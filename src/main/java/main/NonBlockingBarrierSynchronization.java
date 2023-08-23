package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NonBlockingBarrierSynchronization extends Thread {
    private final int id;
    private final List<BarrierTask> tasks;
    private ArrayList<NonBlockingBarrierSynchronization> threads;
    private volatile boolean running = true;

    public NonBlockingBarrierSynchronization(int id) {
        this.id = id;

        tasks = new ArrayList<>();
        for (int x = 0 ; x < 3; x++) {
            BarrierTask barrierTask = new BarrierTask(id, x, tasks);
            barrierTask.wait = true;
            tasks.add(barrierTask);
        }
        tasks.get(0).wait = false;
        BarrierTask resetTask = new ResetTask(id, 3, tasks);
        tasks.add(resetTask);
        tasks.get(3).wait = true;
//        BarrierTask waitTask = new BarrierTask(id, 4, tasks);
//        tasks.add(waitTask);
//        tasks.get(4).wait = true;
        // tasks.get(4).arrived = true;
    }

    public static void main(String args[]) throws InterruptedException {
        int threadCount = 12;
        long seconds = 20;

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
        long n = 0;
        for (NonBlockingBarrierSynchronization thread : threads) {
            for (BarrierTask task : thread.tasks) {
                n += task.n;
            }
        }
        System.out.println(String.format("Requests %d", n));
        System.out.println(String.format("Requests per second %d", n / seconds));

    }
    public void run() {


        while (running) {
            for (int x = 0 ; x < tasks.size(); x++) {
                BarrierTask task = tasks.get(x);
                if (task.wait && task.available) {
                    int arrived = 0;

                    int previous = (x > 0 ? x - 1 : tasks.size() - 1 );

                    for (NonBlockingBarrierSynchronization thread : threads) {
                        if (thread.tasks.get(previous).arrived.get() == task.arrived.get()) {
                            arrived++;
                        }
//                         System.out.println(String.format(" %d %d %d", arrived, thread.tasks.get(previous).arrived.get(), tasks.get(previous).arrived.get()));


                    }
//                        System.out.println(String.format("stopped on %d", same));

                    if ((arrived == 0) || arrived == threads.size()) {
//                         tasks.get(previous).arrived = false;
                        task.available = false;
                        task.run();
                        task.arrive();
//
                        break;
                    }
                    else if (arrived != threads.size()) {
                        // we cannot continue
//                        System.out.println(String.format("we cannot continue %d arrived %d at task %d", arrived, threads.size(), x));
                        break;
                    } else {}
                } else if (task.available) {

                    task.available = false;
                    task.run();
                    task.arrive();

                } else {

                }
            }

        }
    }
    private void setThreads(ArrayList<NonBlockingBarrierSynchronization> threads) {
        this.threads = threads;
    }

    private static class BarrierTask {
        private final List<BarrierTask> tasks;
        public boolean departed;
        public boolean reset;
        private volatile boolean wait;
        private AtomicInteger arrived = new AtomicInteger();
        private final int id;
        public final int task;
        public volatile boolean available;
        private int n;

        public BarrierTask(int id, int task, List<BarrierTask> tasks) {
            this.tasks = tasks;
            this.id = id;
            this.task = task;
            this.available = true;
            this.wait = false;
            this.departed = false;
        }
        public void run() {
            // System.out.println(String.format("Thread %d arrived Task %d", this.id, this.task));

            n++;

        }

        public void arrive() {
            this.arrived.incrementAndGet();
        }

        public void depart() {
            this.departed = true;
        }
    }

    private class ResetTask extends BarrierTask {
        public ResetTask(int id, int task, List<BarrierTask> tasks) {
            super(id, task, tasks);
            super.reset = true;

        }
        public void run() {
            super.n++;
//            System.out.println(String.format("Thread %d arrived Task %d (reset)", id, task));
            for (int x = 0 ; x < tasks.size(); x++) {

                BarrierTask task = tasks.get(x);
                task.wait = true;
                task.available = true;
                task.arrived.incrementAndGet();
            }

// interlocking, lets move into place, gravity
            // an be safely single threaded but concurrent


        }
    }


}

