package main;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReferencePassing extends Thread {

    private Map<Reference, Integer> totals;
    private final Map<Reference, Integer>  availables;

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 100;
        int threadId = 0;
        Map<String, Reference> datas = new HashMap<>();
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        datas.put("main", data.reference);
        List<ReferencePassing> refPasses = new ArrayList<>();
        ReferencePassing copier = new ReferencePassing("copier", threadId++);
        refPasses.add(copier);
        for (int i = 1 ; i < threadCount; i++) {
            ReferencePassing thread = new ReferencePassing("worker", threadId++);
            refPasses.add(thread);

        }
        copier.setThreads(new ArrayList<>(refPasses));
        copier.addAvailable(data.reference);
        for (int i = 0 ; i < threadCount; i++) {
            refPasses.get(i).setThreads(new ArrayList<>(refPasses));
            refPasses.get(i).addRequest(data.reference);
        }
        for (int i = 0 ; i < threadCount ; i++) {
            refPasses.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
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
    public ReferencePassing(String mode, int threadId) {
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
        ReferenceProgress value = new ReferenceProgress(reference.data.value, reference, reference);
        value.end = false;
        rp.put(reference, value);
        this.requests.add(rp);
        this.published.add(rp);
        threads.get(0).availables.put(reference, 1);
        totals.put(reference, 0);
        return reference;
    }

    public void run() {
        List<Reference> removals = new ArrayList<>();

        Map<Reference, Integer> assigned = new HashMap<>();
        Map<Reference, Integer> handled = new HashMap<>();
        while (this.running) {
            if (mode.equals("worker")) {
                if (answers.size() > 0 && answers.get(0).size() > 0) {
                    System.out.println("Running");
                    List<Reference> activeReference = null;

                    writeLock.writeLock().lock();
                    activeReference = answers.get(0);
                    Map<Reference, ReferenceProgress> activeAnswers = requests.get(0);
                    writeLock.writeLock().unlock();
                    HashMap<Reference, ReferenceProgress> newRequests = new HashMap<>();
                    for (Reference reference : activeReference) {
//                            if (handled.containsKey(reference)) {
//                                removals.add(reference);
//                                continue;
//                            }
                            if (!activeAnswers.containsKey(reference)) {
                                System.out.println(String.format("doesn't exist in answers %d", reference.data.value));
                            }
                            System.out.println(String.format("Reading %d", reference.data.value));

                            // totals.put(reference, 0);
                            writeLock.writeLock().lock();

                            writeLock.writeLock().unlock();
                            Reference previous = reference;
                            Reference nextItem = reference.next();
                            removals.add(reference);

                            // removals.add(reference);


                            while (nextItem != null) {


//                                System.out.println(String.format("%d Reading item", threadId));

                                // addRequest(nextItem);
                                int last = 0;
                                if (totals.containsKey(previous)) {
                                    last = totals.get(previous);
                                }
                                totals.put(nextItem, last + 1);
                                activeAnswers.get(reference).incoming = previous;
                                activeAnswers.put(nextItem, new ReferenceProgress(0, activeAnswers.get(reference).past, previous));
                                newRequests.put(nextItem,
                                        activeAnswers.get(reference));


//                                removals.add(nextItem);
                                previous = nextItem;
                                nextItem = nextItem.next();
                                writeLock.writeLock().lock();
                                // available.put(nextItem, 0);

                                writeLock.writeLock().unlock();
                            }
                        if (nextItem == null) {
//                            System.out.println("Adding");
                            ReferenceProgress oldReferenceProgress = activeAnswers.get(reference);


                            ReferenceProgress newReferenceProgress = oldReferenceProgress;
//                            activeAnswers.get(reference).end = false;
                            int nextVal = 1;
                            if (totals.containsKey(previous)) {
                                nextVal = totals.get(previous);
                            }
                            Reference newRef = oldReferenceProgress.past.insert(nextVal);
                            // totals.put(previous, 0);
                            // totals.put(oldReferenceProgress.past, 0);
                            // addRequest(reference);
//                            removals.add(reference);
                            count++;
                            // activeAnswers.remove(reference);
                            writeLock.writeLock().lock();
//                            available.put(newRef, 0);
                            writeLock.writeLock().unlock();
                            System.out.println(String.format("%s is now available", newReferenceProgress.past.data.value));
                            handled.put(reference, 1);
                            handled.put(newRef, 1);
                            writeLock.writeLock().lock();
                            totals.clear();

                            available.put(oldReferenceProgress.past, 0);
                            writeLock.writeLock().unlock();
                        }
                            // activeAnswers.remove(reference);

//                            Reference next = reference.next();
//                            if (next == null) {
//
//                                continue;
//                            }
//                            System.out.println("Read");
//                            ReferenceProgress value = activeAnswers.get(reference);
//                            value.total = value.total + next.data.value;
//                            activeAnswers.remove(reference);
//                            newRequests.put(next, value);

                    }
                    if (newRequests.size() > 0) {
                        published.add(newRequests);
                        requests.add(newRequests);
                    }
                    for (Reference removal : removals) {
                        activeReference.remove(removal);
                    }
                    removals.clear();
                    if (answers.size() > 0 && answers.get(0).size() == 0) {
                        answers.remove(answers.get(0));
                    }
                    if (activeAnswers.size() == 0) {
                        System.out.println("empty answers");
                        readLock.writeLock().lock();
                        requests.remove(activeAnswers);
                        published.remove(activeAnswers);
                        readLock.writeLock().unlock();
                    }
                } else {
//                    System.out.println("Answers is empty");
                }

            }
            if (mode.equals("copier")) {

                for (ReferencePassing other : threads) {
                    other.writeLock.writeLock().lock();
                    for (Map.Entry<Reference, Integer> entry : other.available.entrySet()) {
                        availables.put(entry.getKey(), 1);
                        assigned.remove(entry.getKey());
                    }
                    other.writeLock.writeLock().unlock();

                }

                for (ReferencePassing thread : threads) {
                    if (thread.requests.size() == 0) {
                        continue;
                    }
                    if (thread.published.size() == 0) {
                        continue;
                    }


                    thread.writeLock.writeLock().lock();
                    Map<Reference, ReferenceProgress> remove = thread.published.get(0);
                    thread.writeLock.writeLock().unlock();
                    List<Reference> answers = new ArrayList<>();
                    boolean found = false;

                    for (Map.Entry<Reference, ReferenceProgress> item : remove.entrySet()) {

                        if (!assigned.containsKey(item.getKey()) && !availables.containsKey(item.getKey())) {
                            answers.add(item.getKey());
                            availables.put(item.getKey(), 1);
                            removals.add(item.getKey());
                            assigned.put(item.getKey(), 1);
                            found = true;
                            System.out.println(String.format("Newly created item Assigned %d to thread %d", item.getKey().data.value, thread.threadId));
                        } else {

                            for (ReferencePassing other : threads) {
                                if (other != thread) {
                                    other.writeLock.writeLock().lock();
                                    for (Map.Entry<Reference, Integer> entry : other.available.entrySet()) {
                                        availables.put(entry.getKey(), 1);
                                    }
                                    other.writeLock.writeLock().unlock();

                                    if (other.available.containsKey(item.getKey())) {
                                        // System.out.println(item.getKey());
                                        answers.add(item.getKey());
                                        other.available.remove(item.getKey());
                                        availables.put(item.getKey(), 1);
                                        removals.add(item.getKey());
                                        assigned.put(item.getKey(), 1);
                                        found = true;
                                        System.out.println(String.format("Assigned %d to thread %d", item.getKey().data.value, thread.threadId));
                                    }
                                }

                            }
                        }
                      if (!found) {
                          // System.out.println(String.format("Could not find reference %s, looking in global reference", item.getKey().data.value));
                          if (availables.containsKey(item.getKey()) && !assigned.containsKey(item.getKey())) {
                              answers.add(item.getKey());
                              System.out.println(String.format("Assigned %d to thread %d from globals", item.getKey().data.value, thread.threadId));
                              found = true;
                          }
                      }

                    }
                    if (found) {
                        thread.published.remove(thread.requests.get(0));
                    }
                    for (Reference removal : removals) {
                        if (thread.published.size() > 0) {
                            thread.published.get(0).remove(removal);
                        }
                    }
                    removals.clear();
                    if (answers.size() == 0) {
                        continue;
                    }
                    System.out.println("Trying to writelock thread");
                    thread.writeLock.writeLock().lock();
                    System.out.println("Acquired writelock");
                    System.out.println(answers);
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
        private Reference incoming;
        public ReferenceProgress(int total, Reference past, Reference incoming) {
            this.total = total;
            this.past = past;
            this.incoming = incoming;
        }
    }
}
