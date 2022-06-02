package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Work extends Thread {

    private final int size;
    private final CountDownLatch stage1;
    private final CountDownLatch stage2;
    private List<Thread> threads;
    private final List<WorkItem> workItems1;
    private final List<WorkItem> workItems2;

    public Work(int size, CountDownLatch stage1, CountDownLatch stage2, List<WorkItem> workItems1, List<WorkItem> workItems2) {
        this.size = size;
        this.stage1 = stage1;
        this.stage2 = stage2;

        this.workItems1 = workItems1;
        this.workItems2 = workItems2;

    }

    public void run() {
        for (WorkItem work : workItems1) {
            work.work();
        }
        stage1.countDown();
        try {
            stage1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Finished stage1");
        stage2.countDown();
        try {
            stage2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Finished stage2");
    }
    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        List<WorkItem> workItems1 = new ArrayList<>();
        List<WorkItem> workItems2 = new ArrayList<>();
        for (int i = 0 ; i < 10000; i++) {
            workItems1.add(new WorkItem(i));
            workItems2.add(new WorkItem(i));
        }
        CountDownLatch stage1 = new CountDownLatch(5);
        CountDownLatch stage2 = new CountDownLatch(5);
        for (int i = 0 ; i < 5; i++) {

            Work work = new Work(5, stage1, stage2, workItems1, workItems2);

            threads.add(work);
        }
        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread work : threads) {
            work.join();
        }

    }

    private static class WorkItem {
        int value;
        public WorkItem(int value) {
            this.value = value;
        }

        public void work() {
            this.value = this.value * 2;
        }
    }
}
