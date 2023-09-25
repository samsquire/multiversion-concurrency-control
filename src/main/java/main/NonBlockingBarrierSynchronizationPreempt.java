package main;

import java.util.*;

public class NonBlockingBarrierSynchronizationPreempt extends Thread {
    private final int threadCount;
    private final int id;
    private final List<BarrierTask> tasks;
    private volatile ArrayList<NonBlockingBarrierSynchronizationPreempt> threads;
    private volatile boolean running = true;
    public Map<BarrierTask, Boolean> scheduled = new HashMap<>();


    public NonBlockingBarrierSynchronizationPreempt
            (int threadCount, int id,
             List<NonBlockingBarrierSynchronizationPreempt> threads) {
        this.threadCount = threadCount;
        this.id = id;



        tasks = new ArrayList<>();
        int bt = 0;
        int tcount = threadCount;
        for (int x = 0; x < tcount; x++) {

            BarrierTask barrierTask = null;
            if (x == tcount - 1) {
                if (id == threadCount - 1) {
                    barrierTask = new StealTask(x, x, tasks, threads, threadCount, x, bt);
                    barrierTask.wait = true;
                    tasks.add(barrierTask);
                    ClearTask clearTask = new ClearTask(x, tasks.size(), tasks, threads, threadCount, tasks.size());
                    barrierTask.wait = true;
                    tasks.add(clearTask);
                } else {
                    System.out.println(String.format("%d %d", x, id));
                    barrierTask = new NullTask(x, x, tasks, threads, threadCount, x);
                    barrierTask.wait = true;
                    tasks.add(barrierTask);
                    barrierTask = new BarrierTask(x, x, tasks, threads, threadCount, x);
                    barrierTask.wait = true;
                    tasks.add(barrierTask);
                }
            } else {
                bt = x;
                barrierTask = new BarrierTask(x, x, tasks, threads, threadCount, x);
                tasks.add(barrierTask);
            }
            barrierTask.wait = true;

        }
        for (BarrierTask task: tasks) {
            task.parent = this;
            task.tcount = tcount;
            scheduled.put(task, false);
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

    public boolean isScheduled(BarrierTask lightWeightThread) {
        return scheduled.get(lightWeightThread);
    }

    public static void main(String args[]) throws InterruptedException {
        int threadCount = 12;
        long seconds = 12;

        List<NonBlockingBarrierSynchronizationPreempt> threads = new ArrayList<>();
        for (int x = 0; x < threadCount; x++) {
            NonBlockingBarrierSynchronizationPreempt thread =
                    new NonBlockingBarrierSynchronizationPreempt(threadCount, x, threads);
            threads.add(thread);
        }
        for (NonBlockingBarrierSynchronizationPreempt thread : threads) {
            for (BarrierTask task : thread.tasks) {
                System.out.println(String.format("%d %d %s", thread.id, task.task, task.getClass()));
            }
        }

        for (int x = 0; x < threadCount; x++) {
            threads.get(x).setThreads(new ArrayList<>(threads));
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).start();
        }
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (NonBlockingBarrierSynchronizationPreempt thread: threads) {
                    thread.prempt();
                }
            }
        }, 5, 5);
        Thread.sleep(seconds * 1000);
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).running = false;
            for (BarrierTask task: threads.get(x).tasks) {
                threads.get(x).scheduled.put(task, false);
            }
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).join();
        }
        long n = 0;
        for (NonBlockingBarrierSynchronizationPreempt thread : threads) {
            for (BarrierTask task : thread.tasks) {
                n += task.n;
//                System.out.println(task.tasks.size());
            }
        }
        System.out.println(String.format("Requests %d", n));
        System.out.println(String.format("Requests per second %d", n / seconds));
        timer.cancel();
    }

    private void prempt() {
        for (BarrierTask task: tasks) {
            scheduled.put(task, false);
        }
        for (BarrierTask task: tasks) {

            for (int loop = 0; loop < task.getLoops(); loop++) {
                task.preempt(loop);
            }
        }


    }

    public void run() {
        int x = 0;
        int arrived = 0;
        while (running) {
            if (x >= tasks.size() - 1) {
                x = 0;
            }
            for (; x < tasks.size(); x = (x + 1)) {
                int previous = (x > 0 ? x - 1 : tasks.size() - 1);
                BarrierTask task = tasks.get(x);
                BarrierTask previousTask = tasks.get(previous);
                if (task.available) {

                    for (int t = 0; t < threads.size(); t++) {
                        if (threads.get(t).tasks.get(previous).arrived == task.arrived) {
                            arrived++;
                        }
//                         System.out.println(String.format(" %d %d %d", arrived, thread.tasks.get(previous).arrived.get(), tasks.get(previous).arrived.get()));


                    }
//                        System.out.println(String.format("stopped on %d", same));
//                    System.out.println(String.format("arrived %d", arrived));
                    if (arrived == 0 || arrived == threads.size()) {
//                         tasks.get(previous).arrived = false;
                        scheduled.put(task, true);
                        task.available = false;
                        task.run();
                        scheduled.put(task, false);
//                        System.out.println(task.getClass());
                        task.arrive();
                        arrived = 0;
                        break;
                    } else {
                        // we cannot continue
//                        System.out.println(String.format("we cannot continue %d arrived %d at task %d", arrived, threads.size(), x));
                        if (previousTask.rerunnable && task.rerunnable) {
                            scheduled.put(task, true);
                            previousTask.run();
                            scheduled.put(task, false);
                        }
                        arrived = 0;

//                         System.out.println(task.getClass());

                        break;
                    }
                }
            }
            // arrived = 0;


        }
    }

    private void setThreads(ArrayList<NonBlockingBarrierSynchronizationPreempt> threads) {
        this.threads = threads;
    }

    private static class BarrierTask {
        private final List<BarrierTask> tasks;
        private final int stealingThread;
        public boolean rerunnable = false;
        public boolean departed;
        public boolean reset;
        public NonBlockingBarrierSynchronizationPreempt parent;
        private volatile boolean wait = true;
        private volatile int arrived;
        private final int id;
        public final int task;
        private final List<NonBlockingBarrierSynchronizationPreempt> threads;
        private final int threadCount;
        public boolean available;
        protected long n;
        private List<List<ThreadWork>> threadWork = new ArrayList<>();
        private List<Long> data = new ArrayList<>(100000);
        int[] values = new int[1];
        int[] limits = new int[1];
        boolean[] preempted = new boolean[1];
        int[] remembered = new int[1];
        int tcount;
        public BarrierTask(int id, int task, List<BarrierTask> tasks,
                           List<NonBlockingBarrierSynchronizationPreempt> threads,
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

            while (this.parent.isScheduled(this)) {
                long e = System.currentTimeMillis();
                for (int x = 0 ; x < 1; x++) {
                    data.add(e);
                }
            }
            parent.scheduled.put(this, false);


//             System.out.println(String.format("Thread %d arrived Task %d", this.id, this.task));

            n++;
        }

        public void preempt(int id) {
            remembered[id] = values[id];
            preempted[id] = true;
            values[id] = limits[id];
        }

        public void arrive() {
            this.arrived++;
        }

        public void depart() {
            this.departed = true;
        }

        public int getLoops() {
            return 1;
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
        private final int bt;

        public StealTask(int id,
                         int x,
                         List<BarrierTask> tasks,
                         List<NonBlockingBarrierSynchronizationPreempt> threads,
                         int threadCount,
                         int stealThread, int bt) {
            super(id, x, tasks, threads, threadCount, stealThread);
            this.bt = bt;
            this.rerunnable = false;

        }

        public void run() {
//            super.n++;
            int size = 0;

            for (int x = 0; x < threadCount; x++) {
                for (int y = 0; y < tcount; y++) {
                    size += threads.get(x).tasks.get(y).data.size();
                    threads.get(x).tasks.get(y).data.clear();
                }
            }
//            List<Long> mydata = new ArrayList<>(size);
            n += size;
//            mydata.sort(Long::compareTo);

//            System.out.println(data.size());
        }

    }

    private class ClearTask extends BarrierTask {
        public ClearTask(int id, int task, List<BarrierTask> tasks, List<NonBlockingBarrierSynchronizationPreempt> threads, int threadCount, int stealingThread) {
            super(id, task, tasks, threads, threadCount, stealingThread);
            rerunnable = false;
        }

        public void run() {
            for (BarrierTask task : tasks) {
                for (List<ThreadWork> works : task.threadWork) {
                    works.clear();
                }
            }
        }
    }

    private class NullTask extends BarrierTask {
        public NullTask(int x,
                        int x1,
                        List<BarrierTask> tasks,
                        List<NonBlockingBarrierSynchronizationPreempt> threads,
                        int threadCount,
                        int x2) {
            super(x, x1, tasks, threads, threadCount, x2);
            rerunnable = false;
        }
        public void run() {

        }
    }

}

