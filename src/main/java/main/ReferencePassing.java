package main;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReferencePassing extends Thread {

    private Map<Reference, Integer> totals;
    private final Map<Reference, Integer>  availables;
    private int lastThread = 0;

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        int threadId = 0;
        Map<String, Reference> datas = new HashMap<>();
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        datas.put("main", data.reference);
        List<ReferencePassing> refPasses = new ArrayList<>();
        ReferencePassing copier = new ReferencePassing(null, "copier", threadId++);
//        refPasses.add(copier);
        for (int i = 0 ; i < threadCount; i++) {
            ReferencePassing thread = new ReferencePassing(copier, "worker", threadId++);
            refPasses.add(thread);

        }
        copier.setThreads(new ArrayList<>(refPasses));
        copier.addAvailable(data.reference);
        refPasses.get(0).setThreads(new ArrayList<>(refPasses));
        for (int i = 0 ; i < threadCount; i++) {
            refPasses.get(i).setThreads(new ArrayList<>(refPasses));
            refPasses.get(i).addRequest(data.reference);
        }
        copier.start();
        for (int i = 0 ; i < threadCount ; i++) {
            refPasses.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        copier.running = false;
        copier.join();
        for (int i = 0 ; i < threadCount; i++) {
            refPasses.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount ; i++) {
            refPasses.get(i).join();
        }
        System.out.println("Finished");
        long end = System.currentTimeMillis();

        long totalRequests = 0;

        for (int i = 0; i < refPasses.size(); i++) {
            totalRequests += refPasses.get(i).count;
        }

        double seconds = (end - start) / 1000.0;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));

        DoublyLinkedList current = data;
        List<Integer> numbers = new ArrayList<>();
        while (current != null) {
            numbers.add(current.value);
            current = current.tail;
        }
        System.out.println(numbers);

    }

    private void addAvailable(Reference reference) {
        this.available.put(reference, 0);
    }

    private void setThreads(List<ReferencePassing> threads) {
        this.threads = threads;
        this.threadCount = threads.size();
    }

    private ReentrantReadWriteLock readLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock writeLock = new ReentrantReadWriteLock();
    private List<ReferencePassing> threads;

    private int count;
    private int threadCount;
    private ReferencePassing copierThread;
    private String mode;
    private int threadId;
    private boolean running = true;
    private List<Map<Reference, ReferenceProgress>> requests;
    private List<Map<Reference, ReferenceProgress>> published;
    private List<List<Reference>> answers;
    private Map<Reference, Integer> available;
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
    public ReferencePassing(ReferencePassing copierThread, String mode, int threadId) {
        this.copierThread = copierThread;
        this.mode = mode;
        this.threadId = threadId;
        this.requests = new ArrayList<>();
        this.published = new ArrayList<>();
        this.available = new HashMap<>();
        this.answers = Collections.synchronizedList(new ArrayList<>());
        this.availables = new HashMap<>();
        this.totals = new HashMap<>();
    }

    public Reference addRequest(Reference reference) {
        HashMap<Reference, ReferenceProgress> rp = new HashMap<>();
        ReferenceProgress value = new ReferenceProgress(reference.data.value, reference, reference, false);
        value.end = false;
        rp.put(reference, value);
        this.requests.add(rp);
        this.published.add(rp);
        copierThread.availables.put(reference, 1);
        totals.put(reference, 0);
        return reference;
    }

    public void run() {
        List<Reference> removals = new ArrayList<>();

        Map<Reference, Integer> assigned = new HashMap<>();
        Map<Reference, Integer> handled = new HashMap<>();
            if (mode.equals("worker")) {
                while (this.running) {
//                    System.out.println(requests.size());
                    if (answers.size() > 0) {
//                        System.out.println(requests.size());
                        List<Reference> publishAvailable = new ArrayList<>();
//                    System.out.println("Running");
                        List<Reference> activeReference = null;

                        writeLock.writeLock().lock();
                        activeReference = answers.get(0);
                        Map<Reference, ReferenceProgress> activeAnswers = requests.get(0);
                        writeLock.writeLock().unlock();
                        HashMap<Reference, ReferenceProgress> newRequests = new HashMap<>();
                        for (Reference reference : activeReference) {
                            if (activeAnswers.get(reference).end) {

                                ReferenceProgress oldReferenceProgress = activeAnswers.get(reference);
                                DoublyLinkedList tail = reference.data.tail;


                                Integer value = totals.get(reference);
                                if (value == null) {
                                    value = 0;
                                }
                                totals.put(reference, value + 1);
                                assert totals.get(reference) != null;

                                // assert totals.get(reference).equals(value) : String.format("%d != %d", totals.get(reference), value);
                                Reference newRef = oldReferenceProgress.past.insert(totals.get(reference));
//                                totals.clear();
                                // totals.put(previous, 0);
                                // totals.put(oldReferenceProgress.past, 0);
                                // addRequest(reference);
//                                answers.remove(0);
                                count++;
//                            available.put(newRef, 0);
//                                 System.out.println(String.format("%s is now available", reference.data.value));
                                handled.put(reference, 1);
                                handled.put(newRef, 1);


                                // removals.add(reference);

                                ReferenceProgress newRequest = activeAnswers.get(reference);
                                newRequest.end = false;
                                newRequest.reachedEnd = false;

                                newRequests.put(newRef, newRequest);
                                activeAnswers.put(newRef, newRequest);
                                // totals.put(activeAnswers.get(reference).past, 0);
                                publishAvailable.add(reference);
                                publishAvailable.add(newRef);

//                                activeAnswers.get(reference).end = false;
                                removals.add(reference);
//                                 activeAnswers.remove(reference);
                                continue;
                                // totals.put(previous, totals.get(reference));
//                            System.out.println("Adding");

                            }
//                            System.out.println("Reading");
                            int last = 0;
                            if (totals.containsKey(reference)) {
                                last = activeAnswers.get(reference).total;
                            }
                            activeAnswers.get(reference).total = last + 1;
                            totals.put(reference, last + 1);
                            Reference previous = reference;
                            Reference nextItem = null;
                            ReferenceProgress past = activeAnswers.get(activeAnswers.get(reference).past);
                            if (!past.reachedEnd) {
                                nextItem = reference.next();
                            }

                            publishAvailable.add(previous);
                            removals.add(reference);


                            if (!past.reachedEnd) {
                                past.reachedEnd = true;
                                while (nextItem != null) {


//                                System.out.println(String.format("%d Reading item", threadId));

                                    // addRequest(nextItem);

                                    activeAnswers.get(reference).incoming = previous;
                                    ReferenceProgress value = activeAnswers.get(previous);
                                    activeAnswers.put(nextItem, value);
                                    newRequests.put(nextItem,
                                            activeAnswers.get(nextItem));
                                    activeAnswers.put(nextItem, activeAnswers.get(nextItem));

                                    removals.add(nextItem);
                                    previous = nextItem;
                                    nextItem = nextItem.next();
                                }
                                if (nextItem == null) {
                                    // activeAnswers.get(activeAnswers.get(reference).past).reachedEnd = true;
                                    ReferenceProgress value = past;
                                            /*
                                            new ReferenceProgress(activeAnswers.get(previous).total,
                                            activeAnswers.get(reference).past,
                                            previous,
                                            true);
                                             */
                                    value.incoming = previous;
                                    value.past = past.past;
                                    value.end = true;
                                    value.reachedEnd = false;
                                    // totals.put(previous, totals.get(previous));
                                    newRequests.put(previous, value);
                                    activeAnswers.put(previous, value);

                                }
                            }
//                            System.out.println("Reached end");


//                            activeAnswers.remove(reference);
                        }

                        if (newRequests.size() > 0) {
//                            System.out.println("Adding requests");
                            writeLock.writeLock().lock();
                            published.add(newRequests);
                            requests.add(newRequests);
                            writeLock.writeLock().unlock();
                        }

                        writeLock.writeLock().lock();
                        for (Reference removal : removals) {
                            activeReference.remove(removal);
                            available.put(removal, 1);
                        }

                        removals.clear();
                        for (Reference reference : publishAvailable) {
                            available.put(reference, 1);
                        }
                        writeLock.writeLock().unlock();
                        publishAvailable.clear();
                        if (answers.size() > 0 && answers.get(0).size() == 0) {
//                            System.out.println("Removing old answers");
                            answers.remove(answers.get(0));
                        }
                        if (activeAnswers.size() == 0) {
//                            System.out.println("empty answers");
                            readLock.writeLock().lock();
                            requests.remove(0);
                            readLock.writeLock().unlock();
                        }
//                        Thread.yield();
                    } else {
//                    System.out.println("Answers is empty");
                    }
                }
                }
                if (mode.equals("copier")) {
                    while (this.running) {
//                        System.out.println("Copying");
                        for (ReferencePassing other : threads) {
                            other.writeLock.writeLock().lock();
                            for (Map.Entry<Reference, Integer> entry : other.available.entrySet()) {
                                if (!availables.containsKey(entry.getKey())) {
                                    availables.put(entry.getKey(), 1);
//                                    System.out.println(String.format("%s is available", entry.getKey()));
                                }
                                assigned.remove(entry.getKey());
                            }
                            other.available.clear();
                            other.writeLock.writeLock().unlock();

                        }

                        for (int lastThread = 0; lastThread < threads.size(); lastThread++) {
                            ReferencePassing thread = threads.get(lastThread);
                            thread.writeLock.writeLock().lock();
                            if (thread.requests.size() == 0) {
//                                System.out.println("No requests");
                                thread.writeLock.writeLock().unlock();

                                continue;
                            }
                            if (thread.published.size() == 0) {
//                                System.out.println("No published requests");

                                thread.writeLock.writeLock().unlock();

                                continue;
                            }



                            Map<Reference, ReferenceProgress> remove = thread.published.get(0);
                            if (remove.size() == 0) {
//                                System.out.println("Empty requests");
                            }
                            thread.writeLock.writeLock().unlock();
                            List<Reference> answers = new ArrayList<>();
                            boolean found = false;

                            for (Map.Entry<Reference, ReferenceProgress> item : remove.entrySet()) {
//                                System.out.println(String.format("Looking for %s", item.getKey()));
                                if (!availables.containsKey(item.getKey())) {
                                    availables.put(item.getKey(), 1);
//                                    System.out.println(String.format("Learned about %s", item.getKey()));
                                }
                                if (availables.containsKey(item.getKey()) && !assigned.containsKey(item.getKey())) {
//                                    System.out.println(String.format("%s Not assigned, assigning to thread %d", item.getKey(), threadId));
                                    answers.add(item.getKey());
                                    removals.add(item.getKey());
                                    assigned.put(item.getKey(), 1);
                                    found = true;

//                                    System.out.println(String.format("Newly created item Assigned %d to thread %d", item.getKey().data.value, thread.threadId));
                                }
                            }
                            if (found) {
//                                System.out.println("Found");
//                                thread.published.remove(thread.requests.get(0));
                            }
                            for (Reference removal : removals) {
                                if (thread.published.size() > 0) {

                                }
                            }
                            removals.clear();
                            if (answers.size() == 0) {
                                continue;
                            }
//                    System.out.println("Trying to writelock thread");
                            thread.writeLock.writeLock().lock();
//                            System.out.println("Setting answers");
//                    System.out.println(answers);
                            thread.answers.add(answers);
                            thread.writeLock.writeLock().unlock();

                        }
                        if (lastThread == threads.size()) {
                            lastThread = 0;
                        }
                }
            }
    }

    public static class ReferenceProgress {
        public boolean end;
        public boolean reachedEnd = false;
        int total;
        private Reference past;
        private Reference incoming;
        public ReferenceProgress(int total, Reference past, Reference incoming, boolean end) {
            this.total = total;
            this.past = past;
            this.incoming = incoming;
            this.end = end;
        }
    }
}
