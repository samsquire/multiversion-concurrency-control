package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.gradle.internal.impldep.com.google.common.collect.ImmutableList.of;

public class ReferencePassing extends Thread {

    private ReentrantReadWriteLock readLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock writeLock = new ReentrantReadWriteLock();
    private List<ReferencePassing> threads;
    private int threadCount;
    private String mode;
    private boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 100;
        Map<String, Reference> datas = new HashMap<>();
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        datas.put("main", data.reference);
        List<ReferencePassing> refPasses = new ArrayList<>();
        ReferencePassing copier = new ReferencePassing("copier");
        refPasses.add(copier);
        for (int i = 1 ; i < threadCount; i++) {
            ReferencePassing thread = new ReferencePassing("worker");
            refPasses.add(thread);

        }
        for (int i = 0 ; i < threadCount; i++) {
            refPasses.get(i).setThreads(new ArrayList<>(refPasses));
        }
        for (int i = 0 ; i < threadCount ; i++) {
            refPasses.get(i).start();
        }
        Thread.sleep(5000);
        for (int i = 0 ; i < threadCount; i++) {
            refPasses.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount ; i++) {
            refPasses.get(i).join();
        }

    }

    private void setThreads(List<ReferencePassing> threads) {
        this.threads = threads;
        this.threadCount = threads.size();
    }

    private List<List<Reference>> readRequests;
    private List<List<Reference>> writeRequests;
    private List<List<Reference>> answers;
    private Map<String, Reference> datas;
    public static class Reference {
        public DoublyLinkedList data;
        public int owner;
        public Reference(DoublyLinkedList data, int owner) {
            this.data = data;
            this.owner = owner;
        }

        public List<Reference> next() {
            if (data.tail != null) {
                return of(data.tail.reference);
            }
            return null;
        }
    }
    public ReferencePassing(String mode) {
        this.mode = mode;
    }

    public void run() {
        while (this.running) {
            if (mode.equals("worker")) {
                if (readRequests.size() > 0 && readRequests.get(0).size() > 0) {
                    List<Reference> activeReference = null;
                    readLock.writeLock().lock();
                    activeReference = readRequests.get(0);
                    readLock.writeLock().unlock();
                    for (Reference reference : activeReference) {
                        readRequests.add(reference.next());
                    }
                }

            }
            if (mode.equals("copier")) {

            }
        }
    }
}
