package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.gradle.internal.impldep.com.google.common.collect.ImmutableList.of;

public class ReferencePassing extends Thread {


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

    private ReentrantReadWriteLock readLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock writeLock = new ReentrantReadWriteLock();
    private List<ReferencePassing> threads;

    private int threadCount;
    private String mode;
    private boolean running = true;
    private List<Map<Reference, ReferenceProgress>> requests;
    private List<List<Reference>> answers;
    private Map<String, Reference> datas;
    public static class Reference {
        public DoublyLinkedList data;
        public int owner;
        public boolean end;

        public Reference(DoublyLinkedList data, int owner) {
            this.data = data;
            this.owner = owner;
        }

        public Reference next() {
            if (data.tail != null) {
                return data.tail.reference;
            }
            return null;
        }

        public Reference insert(int value) {
            DoublyLinkedList insert = this.data.insert(value);
            return insert.reference;
        }
    }
    public ReferencePassing(String mode) {
        this.mode = mode;
    }

    public void run() {
        while (this.running) {
            if (mode.equals("worker")) {
                if (answers.size() > 0 && answers.get(0).size() > 0) {
                    List<Reference> activeReference = null;
                    readLock.writeLock().lock();
                    activeReference = answers.remove(0);
                    Map<Reference, ReferenceProgress> activeAnswers = requests.get(0);
                    readLock.writeLock().unlock();
                    HashMap<Reference, ReferenceProgress> newRequests = new HashMap<>();
                    for (Reference reference : activeReference) {
                        if (reference.end == true) {
                            reference.insert(activeAnswers.get(reference).total + 1);
                        }
                         else if (activeAnswers.containsKey(reference)) {
                            Reference next = reference.next();
                            newRequests.put(next, new ReferenceProgress(reference.data.value, reference, next));
                        } else {
                            Reference current = reference.next();
                            while (current != null) {
                                newRequests.put(current,
                                        activeAnswers.get(reference));
                                current = reference.next();
                                if (current == null) {
                                    activeAnswers.get(reference).end = true;
                                }
                            }

                        }
                    }
                    requests.add(newRequests);
                    if (activeAnswers.size() == 0) {
                        readLock.writeLock().lock();
                        requests.remove(activeAnswers);
                        readLock.writeLock().unlock();
                    }
                }

            }
            if (mode.equals("copier")) {
                for (ReferencePassing thread : threads) {
                    thread.writeLock.writeLock().lock();
                    Map<Reference, ReferenceProgress> remove = thread.requests.remove(0);
                    thread.writeLock.writeLock().unlock();
                    List<Reference> answers = new ArrayList<>();
                    for (Map.Entry<Reference, ReferenceProgress> item : remove.entrySet()) {
                        answers.add(item.getKey());
                    }
                    thread.writeLock.writeLock().lock();
                    thread.answers.add(answers);
                    thread.writeLock.writeLock().unlock();

                }
            }
        }
    }

    public static class ReferenceProgress {
        public boolean end;
        int total;
        private final Reference past;
        private final Reference incoming;
        public ReferenceProgress(int total, Reference past, Reference incoming) {
            this.total = total;
            this.past = past;
            this.incoming = incoming;
        }
    }
}
