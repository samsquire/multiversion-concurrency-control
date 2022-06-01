package main;

import java.util.ArrayList;
import java.util.List;

public class Work extends Thread {
    private static int stage1;
    private final int size;
    private final List<WorkItem> workItems;

    public Work(int size, List<WorkItem> workItems) {
        this.size = size;
        this.workItems = workItems;
        this.stage1 = size;
    }

    public void run() {
        for (WorkItem work : workItems) {
            work.work();
        }
        while (stage1 > 0) {
            synchronized (workItems) {
                workItems.notifyAll();
                try {
                    workItems.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                workItems.notifyAll();
                stage1--;
            }
        }
        System.out.println("Finished");
    }
    public static void main(String[] args) throws InterruptedException {
        List<Work> threads = new ArrayList<>();
        List<WorkItem> workItems = new ArrayList<>();
        for (int i = 0 ; i < 10000; i++) {
            workItems.add(new WorkItem(i));
        }
        for (int i = 0 ; i < 5; i++) {
            Work work = new Work(5, workItems);
            work.start();
            threads.add(work);
        }

        for (Work work : threads) {
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
