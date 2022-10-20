package main;

import java.util.*;

public class Synchronizer extends Thread {

    public int counter;
    private int acksEnd;
    private int currentAck;

    private int NEITHER = -1;
    private final int size;
    private Synchronizer main;
    private DoublyLinkedList data;
    private final int id;
    private final boolean synchronizer;
    private int threadsSize;
    private ArrayList<Synchronizer> threads;
    private volatile boolean running = true;
    private volatile int[] announces;
    private volatile int[] acks;
    private Map<Integer, Announcement> announcements;
    private Map<Integer, Announcement> pending = new HashMap<>();
    private int announce;
    private boolean updating = false;
    private volatile Announcement[] callbacks;
    private volatile int[] readingCallbacks;
    private int currentCallback = 0;
    private int callbackStart = 0;
    private int announceStart;
    private int announceIndex = 0;
    public int callbackCurrent;
    private int otherCurrentAnnounce;
    private List<Synchronizer> waitingFor = new ArrayList<>();
    private int[] created;


    public Synchronizer(int id, boolean synchronizer, DoublyLinkedList data, int size, Synchronizer main) {
        this.id = id;

        this.synchronizer = synchronizer;
        this.size = size;
        this.main = main;
        this.counter = 0;
        this.currentAck = 0;
        this.acksEnd = 0;
        this.announce = 0;
        this.data = data;
        this.announcements = new HashMap<>(1000);
    }


    public static void main(String[] args) throws InterruptedException {

        int numberSynchronizers = 1;
        int numberThreads = 1000;
        int size = 200;
        int id = 0;
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        List<Synchronizer> threads = new ArrayList<>();
        List<Synchronizer> synchronizers = new ArrayList<>();

        for (int i = 0; i < numberSynchronizers; i++) {
            System.out.println(String.format("Creating synchronizer %d", id));
            Synchronizer syncer = new Synchronizer(id++, true, data, size, null);
            threads.add(syncer);
            synchronizers.add(syncer);
        }
        for (int i = 0; i < numberThreads; i++) {
            System.out.println(String.format("Creating thread %d", id));

            Synchronizer syncer = new Synchronizer(id++, false, data, size, synchronizers.get(0));
            threads.add(syncer);
        }
        for (Synchronizer syncer : threads) {
            syncer.setThreads(new ArrayList<>(threads));
        }
        System.out.println("Starting test");
        long start = System.currentTimeMillis();
        for (Synchronizer syncer : threads) {
            syncer.start();
        }
        Thread.sleep(5000);
        for (Synchronizer syncer : threads) {
            syncer.running = false;
        }
        for (Synchronizer syncer : threads) {
            syncer.join();
        }
        System.out.println("Finished");
        long end = System.currentTimeMillis();
        ArrayList<Integer> forwardIteration = new ArrayList<>();
        DoublyLinkedList current = data;
        DoublyLinkedList previous = null;
        System.out.println("Forward iteration");
        while (current != null) {
            forwardIteration.add(current.value);
            previous = current;
            if (current.tail != null) {
                assert current.tail.head == current;
            }
            current = current.tail;

        }
        ArrayList<Integer> backwardIteration = new ArrayList<>();
        current = previous;
        System.out.println("Backward iteration");
        HashMap<Integer, Boolean> found = new HashMap<>();
        while (current != null) {
            backwardIteration.add(current.value);
            assert !found.containsKey(current.value) : current.value;
            found.put(current.value, true);
            // System.out.println(current.value);
            current = current.head;

        }
        // System.out.println(forwardIteration);
        // System.out.println(backwardIteration);
        System.out.println(forwardIteration.size());
        Collections.reverse(backwardIteration);
        System.out.println(backwardIteration.size());
        // assert forwardIteration.size() == backwardIteration.size();
        // assert forwardIteration.equals(backwardIteration);


        double seconds = (end - start) / 1000.0;
        int totalRequests = synchronizers.get(0).counter;
        for (Synchronizer synchronizer : threads) {
            totalRequests = totalRequests + synchronizer.counter;
        }
        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));

    }

    public void run() {
        int lastSynchronizer = 0;
        HashSet<Synchronizer> blocked = new HashSet<>();
        while (running) {
            if (synchronizer) {

                if (waitingFor.size() > 0) {
                    boolean subcheck = false;
                    boolean fail = false;

                    int targetMode = 0;

                    for (int j = 0; j < id; j++) {
                        if (readingCallbacks[j] == targetMode) {
                            fail = true;

                            break;
                        } // data.reading test
                    }
                    for (int j = id + 1; j < threadsSize; j++) {
                        if (readingCallbacks[j] == targetMode) {
                            fail = true;
                            break;
                        } // data.reading test
                    }
                    // data.reading loop


                    if (!fail) {
                        readingCallbacks[id] = targetMode;

                        for (int j = threadsSize - 1; j >= 0; j--) {
                            if (j != id && readingCallbacks[j] == targetMode) {


                                subcheck = true;
                                break;
                            } // data.reading check
                        } // data.reading loop

                        if (!subcheck) {
                            for (int j = 0; j < threadsSize; j++) {
                                if (j != id && readingCallbacks[j] == targetMode) {


                                    subcheck = true;
                                    break;
                                } // data.reading check
                            } // data.reading loop

                        }
                        if (!subcheck) {
                            assert readingCallbacks[id] == targetMode;



                            for (int callbackCurrent = 0; callbackCurrent < callbacks.length; callbackCurrent++) {
                                if (callbacks[callbackCurrent] == null) {
                                    // synchronizer.callbackCurrent = 0;
                                    continue;
                                }
                                if (callbacks[callbackCurrent] != null) {
                                    // System.out.println("Found callback");
                                    Announcement announcement = callbacks[callbackCurrent];
                                    // synchronizer.pending.remove(synchronizer.callbacks[synchronizer.callbackCurrent]);
                                    callbacks[callbackCurrent] = null;
                                    waitingFor.remove(announcement.synchronizer);
                                    counter++;


                                }


                            }


                            readingCallbacks[id] = NEITHER;
                            // System.out.println("Successful exclusive execute");


                            Thread.yield();
                        } else {
                            readingCallbacks[id] = NEITHER;
                        }

                    } // subcheck doubly safe
                    else {
                        readingCallbacks[id] = NEITHER;
                    }




                        // System.out.println("Checking for callbacks");


                }
                if (waitingFor.size() > 0) {
                    // System.out.println(String.format("Waiting for %s", waitingFor));

                    continue;
                }
                // System.out.println("Scheduling work");
                for (int a = 0; a < threads.size(); a++) {
                    int syncId = (lastSynchronizer + a) % threadsSize;
                    lastSynchronizer = syncId;
                    Synchronizer synchronizer = threads.get(syncId);
                    if (synchronizer.updating) {
                        continue;
                    }



                    boolean cancel = false;
                    for (; synchronizer.announceIndex < synchronizer.announces.length; synchronizer.announceIndex++) {
                        int announceId = synchronizer.announces[synchronizer.announceIndex];
                        if (announceId == NEITHER) {
                            synchronizer.announceIndex++;
                            // synchronizer.announceIndex = 0;

                            break;
                        }
                        boolean safe = true;
                        Announcement value = synchronizer.announcements.get(announceId);
                        Announcement thisAnnounce = value;
                        for (int o = 0; o < threads.size(); o++) {
                            int otherSyncId = o;

                            Synchronizer other = threads.get(otherSyncId);
                            if (other.updating) {
                                continue;
                            }
                            if (synchronizer != other) {
                                for (; other.otherCurrentAnnounce < other.announces.length; other.otherCurrentAnnounce++) {
                                    int otherAnnounceId = other.announces[other.otherCurrentAnnounce];
                                    if (otherAnnounceId == NEITHER) {
                                        // other.otherCurrentAnnounce = 0;
                                        continue;
                                    }
                                    if (otherAnnounceId != NEITHER) {
                                        Announcement otherAnnounce = other.announcements.get(otherAnnounceId);

                                        if (otherAnnounce == null) {

                                            // System.out.println(String.format("Error %d %d", otherAnnounceId, other.id));
                                        }

                                        if (otherAnnounce.key.equals(thisAnnounce.key)
                                                && otherAnnounce.item.equals(thisAnnounce.item)) {
                                            // System.out.println("Conflict");

                                            thisAnnounce.queue(otherAnnounce);
//                                            System.out.println("Not safe");

                                        }
                                    }
                                }
                            }
                        } // other search
                        if (safe) {
                            // System.out.println("SAFE");

                            // System.out.println(String.format("Removing %d %d %d %d", value.synchronizer.id, announceId, synchronizer.announces[synchronizer.announceIndex], synchronizer.announceIndex));
                            assert value != null;
                            synchronizer.pending.put(announceId, synchronizer.announcements.get(announceId));
                            synchronizer.announcements.remove(announceId);

                            synchronizer.acks[synchronizer.currentAck] = announceId;
                            synchronizer.currentAck = (synchronizer.currentAck + 1) % size;

                            synchronizer.announces[synchronizer.announceIndex] = NEITHER;

                            waitingFor.add(synchronizer);
                            cancel = true;
                            break;


                        }


                    }
                    if (synchronizer.announceIndex >= size) {
                        synchronizer.announceIndex = 0;
                    }


                    if (cancel) {
                        // System.out.println("Cancelled");
                        break;
                    }
                }
            }
            if (!synchronizer) {
                if (announcements.values().size() == 0) {
                    this.updating = true;
                    StringBuilder sb = new StringBuilder();
                    sb.append("newItem.head");
                    sb.append("head");
                    int key = sb.toString().hashCode();
                    for (int i = 0; i < created.length; i++) {
                        // System.out.println("Creating announcement");
                        // System.out.println(String.format("Creating %d %d", announce, id));

                        this.announcements.put(announce, new Announcement(this, announce, data, key, new Runnable() {

                            @Override
                            public void run() {
                                int nextValue = 0;
                                if (data.tail == null) {
                                    nextValue = Integer.MIN_VALUE;
                                } else {
                                    nextValue = data.tail.value;
                                }
                                data.insert(nextValue + 1);
                            }
                        }));
//                    System.out.println(String.format("Created announcement %d %d", announce, id));


                        // System.out.println(String.format("Setting %d to %d", currentAnnounce, announce));

                        created[i] = announce;
                        announce = (announce + 1);
                    }
                    this.updating = false;

                    boolean subcheck = false;
                    boolean fail = false;

                    int targetMode = 0;

                    for (int j = 0; j < id; j++) {
                        if (data.reading[j] == targetMode) {
                            fail = true;

                            break;
                        } // data.reading test
                    }
                    for (int j = id + 1; j < threadsSize; j++) {
                        if (data.reading[j] == targetMode) {
                            fail = true;
                            break;
                        } // data.reading test
                    }
                    // data.reading loop


                    if (!fail) {
                        data.reading[id] = targetMode;

                        for (int j = threadsSize - 1; j >= 0; j--) {
                            if (j != id && data.reading[j] == targetMode) {


                                subcheck = true;
                                break;
                            } // data.reading check
                        } // data.reading loop

                        if (!subcheck) {
                            for (int j = 0; j < threadsSize; j++) {
                                if (j != id && data.reading[j] == targetMode) {


                                    subcheck = true;
                                    break;
                                } // data.reading check
                            } // data.reading loop

                        }
                        if (!subcheck) {
                            assert data.reading[id] == targetMode;



                            for (int announceIndex = 0; announceIndex < created.length; announceIndex++) {
                                if (created[announceIndex] != NEITHER) {
                                    int announceId = created[announceIndex];
                                    Announcement announcement = this.announcements.get(announceId);
                                    // System.out.println(String.format("Acknowledgement %d %d", id, acks[i]));
                                    // assert this.pending.get(acks[i]) != null : String.format("%d %d doesn't exist", id, acks[i]);
//                                    System.out.println(String.format("Running %d", id));
                                    announcement.runnable.run();
                                    counter++;

                                    this.created[announceIndex] = NEITHER;
                                    this.announcements.remove(announceId);
                                }

                            }

                            data.reading[id] = NEITHER;
                            // System.out.println("Successful exclusive execute");


                            Thread.yield();
                        } else {
                            failAndAnnounce();
                        }

                    } // subcheck doubly safe
                    else {
                        failAndAnnounce();
                    }
                } else {
                    // System.out.println("Checking for acks");
                    boolean queued = false;
                    for (int i = 0; i < acks.length; i++) {
                        if (acks[i] != NEITHER) {
                            queued = true;
                            break;
                        }

                    }
                    if (!queued) {
                        Thread.yield();
                        // System.out.println("No scheduled work");
                        continue;
                    }
//                    System.out.println("Has acks");


                    boolean subcheck = false;
                    boolean fail = false;
                    boolean success = false;
                    int targetMode = 0;

                    for (int j = 0; j < id; j++) {
                        if (data.reading[j] == targetMode) {
                            fail = true;

                            break;
                        } // data.reading test
                    }
                    for (int j = id + 1; j < threadsSize; j++) {
                        if (data.reading[j] == targetMode) {
                            fail = true;
                            break;
                        } // data.reading test
                    }
                    // data.reading loop


                    if (!fail) {
                        data.reading[id] = targetMode;

                        for (int j = threadsSize - 1; j >= 0; j--) {
                            if (j != id && data.reading[j] == targetMode) {


                                subcheck = true;
                                break;
                            } // data.reading check
                        } // data.reading loop

                        if (!subcheck) {
                            for (int j = 0; j < threadsSize; j++) {
                                if (j != id && data.reading[j] == targetMode) {


                                    subcheck = true;
                                    break;
                                } // data.reading check
                            } // data.reading loop

                        }
                        if (!subcheck) {
                            assert data.reading[id] == targetMode;
                            success = true;



                            for (int i = 0; i < acks.length; i++) {

                                if (acks[i] != NEITHER) {
                                    Announcement announcement = this.pending.get(acks[i]);
                                    // System.out.println(String.format("Acknowledgement %d %d", id, acks[i]));
                                    // assert this.pending.get(acks[i]) != null : String.format("%d %d doesn't exist", id, acks[i]);


                                    announcement.runnable.run();

                                    main.currentCallback = (main.currentCallback + 1) % size;
                                    main.callbacks[main.currentCallback] = announcement;

                                    this.acks[i] = NEITHER;


                                }


                            }
                            data.reading[id] = NEITHER;
                        } else {
                            data.reading[id] = NEITHER;
                        }


                    } else {
                        data.reading[id] = NEITHER;
                    }
                } // create-or-execute
            }
        }
    }

    private void failAndAnnounce() {
        this.updating = true;
        // queue for serialisation
        data.reading[id] = NEITHER;
        for (int i = 0; i < created.length; i++) {
            announces[i] = created[i];
        }
        this.updating = false;
    }

    private void setThreads(ArrayList<Synchronizer> threads) {
        this.threadsSize = threads.size();
        this.threads = threads;
        this.callbacks = new Announcement[size];
        this.acks = new int[size];
        this.announces = new int[size];
        this.created = new int[size];
        Arrays.fill(callbacks, null);
        Arrays.fill(acks, NEITHER);
        Arrays.fill(announces, NEITHER);
        this.data.reading = new int[threadsSize];
        Arrays.fill(data.reading, NEITHER);
        this.readingCallbacks = new int[threadsSize];
        Arrays.fill(readingCallbacks, NEITHER);
    }

    private class Announcement {
        private Synchronizer synchronizer;
        private final int announceId;
        public Object item;
        public Integer key;
        private Runnable runnable;
        private ArrayList<Announcement> queued;

        public Announcement(Synchronizer synchronizer, int announceId, Object item, Integer key, Runnable runnable) {
            this.synchronizer = synchronizer;
            this.announceId = announceId;
            this.item = item;
            this.key = key;
            this.runnable = runnable;
            this.queued = new ArrayList<>();
        }

        public void queue(Announcement other) {
            this.queued.add(other);
        }
    }
}
