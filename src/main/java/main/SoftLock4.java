package main;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SoftLock4 extends Thread {
    private final ReentrantReadWriteLock.WriteLock lock;
    private DoublyLinkedList list;
    private int id;
    private int locksPerThread;
    private int sublocks;
    private Map<String, Lock> proted;
    private List<SoftLock4> threads;
    private boolean running = true;

    private Map<String, List<Lock>> waiting = new HashMap<>();
    private ArrayList<ArrayList<Lock>>[] incoming;
    private long n;
    private int nextFree;
    private ArrayList<Lock> bigLocks;
    private Map<String, List<List<Lock>>> regionLocks;
    private ActiveRegion activeRegion;

    public SoftLock4(int id,
                     int locksPerThread,
                     int sublocks,
                     Map<String, Lock> proted,
                     List<String> regions, ActiveRegion activeRegion) {
        this.activeRegion = activeRegion;
        this.list = list;
        this.id = id;
        this.locksPerThread = locksPerThread;
        this.sublocks = sublocks;
        this.proted = proted;
        this.lock = new ReentrantReadWriteLock().writeLock();
        this.bigLocks = new ArrayList<Lock>();
        this.regionLocks = new HashMap<>();
        for (String region : regions) {
            this.waiting.put(region, new ArrayList<>());
        }

    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        long seconds = 20;
        int locksSize = 100;
        int sublocks = 10;

        ActiveRegion activeRegion = new ActiveRegion("a");
        int locksPerThread = locksSize / threadCount;
        List<SoftLock4> threads = new ArrayList<>();
        Map<String, Lock> proted = new HashMap<>();
        ArrayList<String> regions = new ArrayList<>();
        regions.add("a");
        regions.add("b");
        for (int x = 0; x < threadCount; x++) {
            SoftLock4 thread = new SoftLock4(x, locksPerThread, sublocks, proted, regions, activeRegion);
            threads.add(thread);
        }
        DoublyLinkedList list = new DoublyLinkedList(0, 0);
        Lock amaster = new Lock(list, 0, null, null, "a", regions, threads.size());
        Lock bmaster = new Lock(list, 0, null, null, "b", regions, threads.size());
        proted.put("a", amaster);
        proted.put("b", bmaster);
        for (int x = 0; x < threadCount; x++) {
            amaster.done[x] = true;
        }
        amaster.master = true;
        bmaster.master = true;
//        bmaster.done = true;


        for (int x = 0; x < threadCount; x++) {
            threads.get(x).setThreads(new ArrayList<>(threads));
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).waiting.get("a").add(amaster);
            threads.get(x).waiting.get("b").add(bmaster);
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(seconds * 1000);
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).running = false;
        }
        for (int x = 0; x < threadCount; x++) {
            threads.get(x).join();
        }
        long end = System.currentTimeMillis();

        long total = 0;
        for (int x = 0; x < threadCount; x++) {
            total += threads.get(x).n;
        }
        long duration = end - start;
        System.out.println(String.format("Requests %d", total));
        System.out.println(String.format("Duration %d", duration / 1000));
        System.out.println(String.format("Requests per second %d", total / seconds));
        System.out.println(String.format("Total full synchronizations %d", (12 * locksSize * sublocks)));
        System.out.println(String.format("Locks across threads processed per second %d", (locksSize * sublocks) / (duration / 1000)));
        for (SoftLock4 thread : threads) {
            for (Lock lock : thread.bigLocks) {
                // System.out.println(lock.users);

            }
        }


    }

    private void setThreads(ArrayList<SoftLock4> threads) {
        this.threads = threads;
        incoming = new ArrayList[threads.size()];
        for (int x = 0; x < threads.size(); x++) {
            incoming[x] = new ArrayList<>();
        }
    }

    public void run() {
        Random rng = new Random();
        List<Lock> removals = new ArrayList<>(1000);


        List<String> regions = new ArrayList<>();
        regions.add("a");
        regions.add("b");
        Map<String, String> nextRegions = new HashMap<>();
        for (int x = 0; x < regions.size(); x++) {
            nextRegions.put(regions.get(x), regions.get((x + 1) % regions.size()));
        }
        for (int x = 0; x < regions.size(); x++) {
            ArrayList<List<Lock>> threadLocks = new ArrayList<>();
            for (int y = 0; y < threads.size(); y++) {
                threadLocks.add(new ArrayList<>());
            }
            regionLocks.put(regions.get(x), threadLocks);
        }


        long total = 0;
        HashSet<Lock> parents = new HashSet<>();
        List<Lock> parentRemovals = new ArrayList<>();

        while (running || total > 2) {

            total = 0;
            for (String region : regions) {
                total += waiting.get(region).size();
            }
            if (running && total == 2) {
                System.out.println("Populating locks");

                for (int x = 0; x < locksPerThread; x++) {
                    int pickedLock = rng.nextInt(threads.size());
                    String region = regions.get(rng.nextInt(regions.size()));
                    SoftLock4 target = threads.get(pickedLock);
                    Lock currentLock = new Lock(new DoublyLinkedList(0, 0), pickedLock, this, target, region, regions, threads.size());
                    bigLocks.add(currentLock);
                    proted.get(region).addLock(region, currentLock);

                    regionLocks.get(region).get(pickedLock).add(currentLock);
                    for (int n = 0; n < sublocks; n++) {
                        int dependency = rng.nextInt(threads.size());
                        SoftLock4 dependencyThread = threads.get(pickedLock);
                        String region2 = regions.get(rng.nextInt(regions.size()));

                        Lock dependencyLock = new Lock(new DoublyLinkedList(0, 0), dependency,
                                this,
                                dependencyThread,
                                region2,
                                regions,
                                threads.size());
                        proted.get(region2).addLock(region2, dependencyLock);
                        regionLocks.get(region2).get(dependency).add(dependencyLock);
                        waiting.get(region2).add(dependencyLock);

                    }

                    waiting.get(region).add(currentLock);
                }
            }


//
//            System.out.println(String.format("%d %s awaiting size %d bwaiting size %d " +
//                            " %d %d %d %d",
//                    id, activeRegion.region, waiting.get("a").size(), waiting.get("b").size(), proted.get("a").submitted.get("a").get(),
//                    proted.get("a").locks.get("a").size(),
//                    proted.get("b").submitted.get("b").get(),
//                    proted.get("b").locks.get("b").size()));
                List<Lock> waitingLocks = waiting.get(activeRegion.region);
                threads.get(id).lock.lock();

                for (Lock lock1 : waitingLocks) {
//
//                    if (lock1.parent.submitted.get(lock1.region).get() == 0) {
//                        parents.add(lock1.parent);
//                        break;
//                    }


                        parents.add(lock1.parent);

                        n++;
                        lock1.list.insert(0);
//                    System.out.println("Master lock completed");

                        // lock1.handled = true;
                        lock1.parent.done[id] = false;
                        String nextRegion = nextRegions.get(lock1.parent.region);
                        // System.out.println(String.format("next region is %s", nextRegion));
//                    lock1.parent.submitted.get(nextRegion).set(0);
//                    lock1.parent.submitted.get(lock1.region).set(0);
//                    lock1.submitted.get(lock1.region).set(0);

                        // System.out.println(String.format("parent lock fulfilled %d", lock1.parent.submitted));

                }
                threads.get(id).lock.unlock();



            for (Lock parent : parents) {
                parentRemovals.add(parent);
                for (int y = 0; y < threads.size(); y++) {
//                System.out.println(String.format("%d Sending locks", y));

                    threads.get(id).lock.lock();

//                                System.out.println(String.format("size %s %s ", nextRegion, regionLocks.get(nextRegion).size()));
                    threads.get(y).incoming[id].add(new ArrayList<>(regionLocks.get(parent.region).get(y)));
                    regionLocks.get(parent.region).get(y).clear();
                    threads.get(id).lock.unlock();

//                                lock1.parent.locks.get(lock1.region).removeAll(waiting);
//                                lock1.parent.locks.get(lock1.region).add(proted.get(lock1.region));
//                                lock1.parent.locks.get(nextRegion).add(proted.get(nextRegion));
                    if (parent.finishes.get(parent.region).get() < threads.size() * threads.size()) {
                        parent.finishes.get(parent.region).incrementAndGet();
                    }

                }
                if (parent.finishes.get(parent.region).get() == threads.size() * threads.size()) {
//                    waiting.get(nextRegions.get(parent.region)).clear();
//                    waiting.get(parent.region).clear();
//                    waiting.get(parent.region).add(proted.get(nextRegions.get(parent.region)));
//                    parent.finishes.get(parent.region).set(0);
//                    waiting.get(nextRegions.get(parent.region)).add(proted.get(parent.region));

                }


            }
            for (Lock parent : parentRemovals) {
                parents.remove(parent);
            }
            parentRemovals.clear();
            for (int x = 0; x < threads.size(); x++) {
                threads.get(x).lock.lock();
                if (!(incoming[x].size() > 0)) {
                    threads.get(x).lock.unlock();
                    continue;
                }
                List<Lock> remove = incoming[x].remove(0);
                threads.get(x).lock.unlock();



                Iterator<Lock> iterator = remove.iterator();
                while (iterator.hasNext()) {
                    Lock item = iterator.next();
                    iterator.remove();
                    item.finish(item.region, id);
                    Lock parent = item.parent;
                    System.out.println(parent.finishes.get(parent.region).get());
                    int size = parent.locks.get(parent.region).size();
                    if (parent.submitted.get(parent.region).get() == size) {

                        System.out.println(String.format("Clearing next region %s", parent.region));
//                        parentRemovals.add(parent);
                        parent.submitted.get(parent.region).set(0);
                        parent.finishes.get(parent.region).set(0);
//                        parents.add(proted.get(nextRegions.get(parent.region)));
//                    parent.locks.get(parent.region).add(proted.get(nextRegions.get(parent.region)));
//                    parent.locks.get(nextRegions.get(parent.region)).add(proted.get(parent.region));
                        System.out.println(String.format("need a reset %s", parent.region));

                        parent.locks.get(parent.region).clear();
//                         parent.locks.get(parent.region).add(proted.get(parent.region));
                        for (int y = 0; y < threads.size(); y++) {
                            threads.get(y).lock.lock();

                            threads.get(y).waiting.get(parent.region).clear();
                            threads.get(y).waiting.get(parent.region).add(proted.get(parent.region));
                            threads.get(y).lock.unlock();



                                System.out.println(String.format("Total reset resume %s", nextRegions.get(parent.region)));
                                // proted.get(activeRegion.region).done[y] = true;
                        }
                                activeRegion.region = nextRegions.get(activeRegion.region);


                    }
                }


            }

        }
    }


    private static class Lock {
        public volatile boolean finished;
        public volatile boolean handled;
        public String region;
        public volatile boolean done[];
        public boolean master;
        public Map<String, AtomicLong> finishes = new HashMap<>();
        private DoublyLinkedList list;
        private int pickedLock;
        private SoftLock4 owner;
        private SoftLock4 target;
        private ReentrantReadWriteLock.WriteLock lock;
        private Map<String, AtomicInteger> submitted;
        private Map<String, List<Lock>> locks;
        private Lock parent;
        private List<Integer> users;

        public Lock(DoublyLinkedList list, int pickedLock, SoftLock4 owner, SoftLock4 target, String region, List<String> regions, int threadsSize) {
            this.list = list;
            this.pickedLock = pickedLock;
            this.owner = owner;
            this.target = target;
            this.region = region;
            this.lock = new ReentrantReadWriteLock().writeLock();
            this.locks = new HashMap<>();
            this.parent = this;
            this.users = new ArrayList<>();
            this.submitted = new HashMap<String, AtomicInteger>();
            for (String region2 : regions) {
                this.locks.put(region2, new ArrayList<>());
                this.submitted.put(region2, new AtomicInteger());
                this.finishes.put(region2, new AtomicLong());
            }
            this.done = new boolean[threadsSize];
            for (int x = 0; x < threadsSize; x++) {
                this.done[x] = false;
            }
        }

        public void addLock(String region, Lock lock) {
            lock.parent = this;
            this.lock.lock();
            this.locks.get(region).add(lock);
            this.lock.unlock();
        }

        public void finish(String region, int id) {
            // parent.addUser(id);
            this.parent.lock.lock();
            parent.submitted.get(region).set(parent.submitted.get(region).get() + 1);
            this.parent.lock.unlock();

            System.out.println(String.format("Finished region region %s id %d submitted %d locks %d alocks %d blocks %d", region, id, parent.submitted.get(region).get(), parent.locks.get(region).size(), parent.locks.get("a").size(), parent.locks.get("b").size()));

        }

        private void addUser(int id) {
            this.users.add(id);
        }

    }

    private static class ActiveRegion {
        private volatile String region;

        public ActiveRegion(String region) {
            this.region = region;
        }
    }
}
