package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NonBlockingBarrierSynchronizationSteal extends Thread {
    private final int threadCount;
    private final int id;
    private final List<BarrierTask> tasks;
    private volatile ArrayList<NonBlockingBarrierSynchronizationSteal> threads;
    private volatile boolean running = true;

    public NonBlockingBarrierSynchronizationSteal
            (int threadCount, int id,
             List<NonBlockingBarrierSynchronizationSteal> threads) {
        this.threadCount = threadCount;
        this.id = id;

        tasks = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            BarrierTask barrierTask = null;
            if (x == id) {
                barrierTask = new StealTask(id, x, tasks, threads, threadCount, x);
            } else {
                barrierTask = new BarrierTask(id, x, tasks, threads, threadCount, x);
            }
            barrierTask.wait = true;
            tasks.add(barrierTask);
        }
        tasks.get(0).wait = false;
        BarrierTask resetTask = new ResetTask(id, tasks.size(), tasks, id);
        tasks.add(resetTask);
        tasks.get(tasks.size() - 1).wait = true;
//        BarrierTask waitTask = new BarrierTask(id, 4, tasks);
//        tasks.add(waitTask);
//        tasks.get(4).wait = true;
        // tasks.get(4).arrived = true;
    }

    public static void main(String args[]) throws InterruptedException {
        int threadCount = 12;
        long seconds = 5;

        List<NonBlockingBarrierSynchronizationSteal> threads = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            NonBlockingBarrierSynchronizationSteal thread =
                    new NonBlockingBarrierSynchronizationSteal(threadCount, x, threads);
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
        for (NonBlockingBarrierSynchronizationSteal thread : threads) {
            for (BarrierTask task : thread.tasks) {
                n += task.n;
//                System.out.println(task.tasks.size());
            }
        }
        System.out.println(String.format("Requests %d", n));
        System.out.println(String.format("Requests per second %d", n / seconds));

    }

    public void run() {
        int x = 0;
        int t = 0;
        int arrived = 0;
        while (running) {
            if (x >= tasks.size() - 1) {
                x = 0;
            }
            for (; x < tasks.size(); x = (x + 1), t = 0) {
                int previous = (x > 0 ? x - 1 : tasks.size() - 1);
                BarrierTask task = tasks.get(x);
                BarrierTask previousTask = tasks.get(previous);
                if (task.available) {

                    for (; t < threads.size() ; t++) {
                        if (threads.get(t).tasks.get(previous).arrived == task.arrived) {
                            arrived++;
                        }
//                         System.out.println(String.format(" %d %d %d", arrived, thread.tasks.get(previous).arrived.get(), tasks.get(previous).arrived.get()));


                    }
//                        System.out.println(String.format("stopped on %d", same));
//                    System.out.println(String.format("%d t arrived %d", t, arrived));
                    if ((arrived == 0 && (t == threads.size())) || arrived == threads.size()) {
//                         tasks.get(previous).arrived = false;
                        task.available = false;
                        task.run();
//                        System.out.println(task.getClass());
                        task.arrive();
                        t = 0;
                        arrived = 0;
                        break;
                    } else {
                        // we cannot continue
//                        System.out.println(String.format("we cannot continue %d arrived %d at task %d", arrived, threads.size(), x));
                        if (previousTask.rerunnable) {
//                             previousTask.run();
                        }
                        t = 0;
                        arrived = 0;

//                         System.out.println(task.getClass());

                        break;
                    }
                }
            }
            // arrived = 0;


        }
    }

    private void setThreads(ArrayList<NonBlockingBarrierSynchronizationSteal> threads) {
        this.threads = threads;
    }

    private static class BarrierTask {
        private final List<BarrierTask> tasks;
        private final int stealingThread;
        public boolean rerunnable;
        public boolean departed;
        public boolean reset;
        private volatile boolean wait;
        private volatile int arrived;
        private final int id;
        public final int task;
        private final List<NonBlockingBarrierSynchronizationSteal> threads;
        private final int threadCount;
        public boolean available;
        protected int n;
        private List<List<ThreadWork>> threadWork = new ArrayList<>();

        public BarrierTask(int id, int task, List<BarrierTask> tasks,
                           List<NonBlockingBarrierSynchronizationSteal> threads,
                           int threadCount, int stealingThread) {
            this.tasks = tasks;
            this.id = id;
            this.task = task;
            this.threads = threads;
            this.threadCount = threadCount;
            this.available = true;
            this.wait = false;
            this.departed = false;
            this.stealingThread = stealingThread;
            for (int x = 0; x < threadCount; x++) {
                threadWork.add(new ArrayList<>());
            }
            this.rerunnable = true;
        }

        public void run() {
//             System.out.println(String.format("Thread %d arrived Task %d", this.id, this.task));
            int size = this.tasks.size();
            for (int b = 0; b < size - 1; b++) {

                for (int t = 0; t < threadCount; t++) {
                    if (b != stealingThread) {
                        threads.get(((id + t) %
                                threads.size())).tasks.get(b)
                                .threadWork.get(id)
                                .add(new ThreadWork(
                                        String.format(
        "%d Work from thread to thread %d", b, id)));
                    }
                }
            }
//            n++;

        }

        public void arrive() {
            this.arrived++;
        }

        public void depart() {
            this.departed = true;
        }

        public class ThreadWork {
            private final String text;

            public ThreadWork(String text) {
                this.text = text;
            }
        }
    }

    private class ResetTask extends BarrierTask {
        public ResetTask(int id, int task, List<BarrierTask> tasks, int stealingThread) {
            super(id, task, tasks, threads, threadCount, stealingThread);
            super.reset = true;
            rerunnable = false;
        }

        public void run() {
//            super.n++;
//            System.out.println(String.format("Thread %d arrived Task %d (reset)", id, task));
            for (int x = 0; x < tasks.size(); x++) {

                BarrierTask task = tasks.get(x);
                task.wait = true;
                task.available = true;
                task.arrived++;
            }

// interlocking, lets move into place, gravity
            // an be safely single threaded but concurrent


        }
    }


    private class StealTask extends BarrierTask {
        public StealTask(int id,
                         int x,
                         List<BarrierTask> tasks,
                         List<NonBlockingBarrierSynchronizationSteal> threads,
                         int threadCount,
                         int stealThread) {
            super(id, x, tasks, threads, threadCount, stealThread);
            rerunnable = false;

        }

        public void run() {
//            super.n++;
            for (int b = 0; b < tasks.size() - 1; b++) {
                for (int x = 0; x < threadCount; x++) {
                    List<List<ThreadWork>> threadWork = threads.get(x).tasks.get(b).threadWork;
//            System.out.println(String.format("Thread %d stealing...%d items", id, threadWork.get(id).size()));


                            for (ThreadWork work : threadWork.get(id)) {
//                         System.out.println(String.format("Stole %s", work.text));
                                n++;
                            }
                        threadWork.get(id).clear();




                }
            }
        }
    }
}

