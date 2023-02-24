package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VirtualJoin extends Thread {



    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        List<VirtualJoin> threads = new ArrayList<>();
        for (int i = 0 ; i < threadCount; i++) {
            VirtualJoin thread = new VirtualJoin(i);
            threads.add(thread);
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).join();
        }
        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        long total = 0;
        for (VirtualJoin join : threads) {
            total += join.n;
        }
        System.out.println(String.format("Total Requests %d", total));
//        System.out.println(String.format("Time elapsed %f", seconds));
//        System.out.println(String.format("Readings %d", readings));
//        System.out.println(String.format("Writings %d", writings));
//        System.out.println(String.format("Total reading+writings %d", sumRW));
        System.out.println(String.format("Requests per second %f", total / seconds));
    }
    private ReadWriteLock lock;
    private volatile JoinHandle[] mailbox;
    private List<JoinHandle> pendings;
    public List<JoinHandle> joinHandles;
    public List<JoinHandle> dones;
    private volatile boolean running = true;
    private int n;
    private final int id;
    private List<VirtualJoin> threads;

    public VirtualJoin(int id) {
        this.id = id;
        this.joinHandles = new ArrayList<>();
        this.dones = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.pendings = new ArrayList<>();
    }

    private void setThreads(ArrayList<VirtualJoin> threads) {
        this.threads = threads;
        this.mailbox = new JoinHandle[threads.size()];

    }

    public void run() {
        Random rng = new Random();
        int pending = 0;
        while (running) {
            if (pending == 0) {
                boolean keepGoing = true;
                int c = 0;
                while (keepGoing) {
                    int nextThreadId = c;
                    if (nextThreadId == id) {
                        c++;
                    }
                    VirtualJoin virtualJoin = threads.get(nextThreadId);

                    JoinHandle handle = virtualJoin.enqueue(this,
                            new RunnableTask() {
                        @Override
                        public void run() {
                            // System.out.println("Runned task");
                        }
                    });
                    if (handle == null) {
                        continue;
                    }
                    pending++;
                    joinHandles.add(handle);
                    c++;
                    if (c < 12) {
                        keepGoing = true;
                    } else {
                        keepGoing = false;
                    }
                }

            }


            for (int x = 0; x < mailbox.length; x++) {
                JoinHandle joinHandle = mailbox[x];
                if (joinHandle != null) {
                    pendings.add(joinHandle);
                    mailbox[x] = null;
                }
            }
            List<JoinHandle> done = new ArrayList<>();

            for (JoinHandle joinHandle : pendings) {
                    joinHandle.done = true;
                    joinHandle.runnable.run();
                    n++;
                    done.add(joinHandle);
            }
            for (JoinHandle joinHandle : done) {
                pendings.remove(joinHandle);
            }
            List<JoinHandle> removals = new ArrayList<>();

            for (JoinHandle joinHandle : joinHandles) {
                if (joinHandle.done) {
                    removals.add(joinHandle);
                }
            }

            for (JoinHandle joinHandle : removals) {
                joinHandles.remove(joinHandle);
                pending--;
            }

        }
    }

    private JoinHandle enqueue(VirtualJoin caller, RunnableTask runnable) {
        JoinHandle joinHandle = new JoinHandle(caller, this, runnable);
        int callerId = caller.id;
        if (mailbox[callerId] != null) {
            return null;
        }
        mailbox[callerId] = joinHandle;
        return joinHandle;
    }

    private class JoinHandle {
        public volatile boolean done = false;
        private VirtualJoin caller;
        private VirtualJoin owner;
        private RunnableTask runnable;


        public JoinHandle(VirtualJoin caller, VirtualJoin owner, RunnableTask runnable) {

            this.caller = caller;
            this.owner = owner;
            this.runnable = runnable;
        }
    }
    public static abstract class RunnableTask implements Runnable {
        private volatile JoinHandle joinHandle;
    }
}
