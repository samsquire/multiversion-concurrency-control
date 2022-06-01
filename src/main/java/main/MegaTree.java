package main;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Integer.max;

public class MegaTree<V> {
    private final int size;
    private final boolean earlyAborts;
    private volatile LRHashMap<String, Integer> committed;
    private volatile int lastCommit;
    public LRHashMap<String, MegaTree<V>> keyCache;
    private Integer version = -1;
    private Integer counter = -1;
    private LRHashMap<String, MegaTree.Transaction> rts;
    private LRHashMap<String, MegaTree.Transaction> wts;
    private V value;
    private volatile LRHashMap<String, LRHashMap<Integer, Version<V>>> database;
    private LRHashMap<String, List<MegaTree.Transaction>> touched;
    private LRHashMap<String, ReentrantReadWriteLock.WriteLock> writeLocks;
    private ReadWriteLock rwlock = new ReentrantReadWriteLock();
    private int precommit;



    public MegaTree(int size, int version, V value, boolean earlyAborts) {
        this.value = value;
        this.version = version;
        this.size = size;
        keyCache = new LRHashMap<>(size);
        database = new LRHashMap<String, LRHashMap<Integer, Version<V>>>(size);
        rts = new LRHashMap<String, MegaTree.Transaction>(size);
        wts = new LRHashMap<String, MegaTree.Transaction>(size);
        touched = new LRHashMap<String, List<MegaTree.Transaction>>(size);
        this.earlyAborts = earlyAborts;
        writeLocks = new LRHashMap<>(size);
        this.committed = new LRHashMap<>(size);

    }

    public void ensure_keys(String... keys) {
        for (String key : keys) {
            System.out.println(String.format("%s doesn't exist, creating", key));
            LRHashMap<Integer, Version<V>> newdata = new LRHashMap<>(size);
            database.put(key, newdata, 0);
            touched.put(key, Collections.synchronizedList(new ArrayList<MegaTree.Transaction>()), 0);

        }
    }

    public boolean isValue() {
        return value != null;
    }

    public Writehandle put(int threadIndex, Transaction transaction, String key, V value, Integer timestamp) {


        String conflictType = "";
        boolean restart = false;
        LRHashMap.Reader reader = new LRHashMap.Reader(threadIndex);
        if (earlyAborts) {
            MegaTree.Transaction peek = rts.get(reader, key);

            if (peek != null && shouldRestart(transaction, peek)) {
                System.out.println(String.format("%d wins against %d", peek, transaction.getTimestamp()));
                restart = true;
            }

            if (restart) {
                // System.out.println(String.format("%d %s failed - early abort", transaction.getTimestamp(), conflictType));
                return null;
            }
        }
        ReentrantReadWriteLock.WriteLock lock = null;
        if (writeLocks.containsKey(reader, key)) {
            lock = writeLocks.get(reader, key);
        } else {
            lock = (ReentrantReadWriteLock.WriteLock) rwlock.writeLock();

            writeLocks.put(key, lock, transaction.getTimestamp());
        }
        System.out.println(String.format("t%d write %d", transaction.getTimestamp(), value));
        transaction.addLock(threadIndex, key, lock);
        lock.lock();
        database.get(reader, key).put(transaction.getTimestamp(), new Version<V>(transaction.getTimestamp(), transaction.getTimestamp(), value), transaction.getTimestamp());
        wts.put(key, transaction, timestamp);

        MegaTree.Writehandle writehandle = new MegaTree.Writehandle(key, timestamp);
        transaction.addWrite(writehandle);
        return writehandle;
    }

    public Read<Version<V>> get(int threadIndex, String key, Transaction transaction) {
        LRHashMap.Reader reader = new LRHashMap.Reader(threadIndex);
        int timestamp = transaction.getTimestamp();
        LRHashMap<Integer, Version<V>> values = database.get(reader, key);
        Integer lastKnownCommit = committed.get(reader, key);

        // System.out.println(String.format("%d Values in database for key %s %s  committed %d", transaction.getTimestamp(), key, values, committed.get(key)));
        ArrayList<Integer> versions = new ArrayList<>(values.keySet(reader));
        values.depart(reader);
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });

        // read your own writes
        if (values.containsKey(reader, transaction.getTimestamp())) {
            return new MegaTree.Read(values.get(reader, transaction.getTimestamp()), transaction.getTimestamp());
        }
        for (Integer version : versions) {
            System.out.println(version);

            // System.out.println(String.format("t%d Inspecting version %d", transaction.getTimestamp(), version));
            if (version <= transaction.getTimestamp()) {


                if (version >= lastCommit && version.equals(lastKnownCommit)) {
                    Version<V> read = values.get(reader, version);


                    MegaTree.Transaction peek = rts.get(reader, key);

                    if (peek != null && (shouldRestart(transaction, peek))) {
                        System.out.println(String.format("t%d %d should win %b", transaction.getTimestamp(), peek.getTimestamp(), peek.getAborted()));
                        Thread.yield();
                        return null;
                    }
                    if (peek != null) {
                        peek.addChallenger(transaction);
                    }


                    rts.put(key, transaction, timestamp);
                    touched.get(reader, key).add(transaction);
                    int peekTimestamp = 0;
                    if (peek != null) {
                        peekTimestamp = peek.getTimestamp();
                    }

                    System.out.println(String.format("%d t%d read %s %d %d", System.nanoTime(), transaction.getTimestamp(), key, read.value, peekTimestamp));
                    if (read == null) {
                        System.out.println("ERROR");
                        throw new IllegalArgumentException();
                    }
                    MegaTree.Read readHandle = new MegaTree.Read(read, lastKnownCommit);
                    transaction.addRead(readHandle);
                    return readHandle;
                } else {
                    System.out.println(String.format("t%d %d Version is not equal to committed %d %d", transaction.getTimestamp(), version, lastKnownCommit, lastCommit));
                }

            } else {
                System.out.println("Ignoring newer value");
            }
        }
        System.out.println(String.format("%d No versions valid", transaction.getTimestamp()));
        return null;
    }

    public void commit(int threadIndex, Transaction<V> transaction) {
        transaction.markPrecommit();


//        synchronized (locks.get(transaction.createLockKey())) {
        boolean restart = false;
        String conflictType = "";

        if (transaction.getTimestamp() < lastCommit) {
            transaction.markRestart(true);
            conflictType = "ahead";
        }


        LRHashMap.Reader reader = new LRHashMap.Reader(threadIndex);
        for (MegaTree.Writehandle writehandle : transaction.getWritehandles()) {
            MegaTree.Transaction peek = rts.get(reader, writehandle.key);
            int peakTimestamp = 0;
            if (peek != null) {
                peakTimestamp = peek.getTimestamp();
            }
            System.out.println(String.format("%d t%d begin commit (peek=%d)", System.nanoTime(), transaction.getTimestamp(), peakTimestamp));
            if (peek != null && (shouldRestart(transaction, peek))) {
                System.out.println(String.format("%d wins against %d", peek.getTimestamp(), transaction.getTimestamp()));
                transaction.markRestart(true);
                conflictType = "read";
                break;
            }

            if (touched.containsKey(reader, writehandle.key)) {
                List<MegaTree.Transaction> transactions = touched.get(reader, writehandle.key);
                System.out.println(String.format("t%d checking touched transactions", transaction.getTimestamp()));
                synchronized (transactions) {
                    for (MegaTree.Transaction other : transactions) {
                        if (shouldRestart(transaction, other)) {
                            transaction.markRestart(true);
                            break;
                        }
                    }
                }
                System.out.println(String.format("t%d finished checking touched transactions", transaction.getTimestamp()));
            }
            System.out.println(String.format("previous committed %d", committed.get(reader, writehandle.key)));
            if (committed.containsKey(reader, writehandle.key) && writehandle.timestamp != null && !committed.get(reader, writehandle.key).equals(writehandle.timestamp)) {
                transaction.markRestart(true);
                conflictType = "commit";
                break;
            }


        }
        System.out.println(String.format("t%d finished committed checks", transaction.getTimestamp()));
        // System.out.println(String.format("%d Checking challengers %d", transaction.getTimestamp(), challengers.size()));

        if (transaction.checkChallengers(this, transaction)) {
            transaction.markRestart(true);
            conflictType = "challenger";
        }

        System.out.println(String.format("t%d Challengers checked", transaction.getTimestamp()));


        precommit = max(transaction.getTimestamp(), precommit);
        // transaction.markSuccessful();


        if (transaction.getRestart()) {
            System.out.println(String.format("t%d Conflict %s Restarting transaction", transaction.getTimestamp(), conflictType));
            abort(threadIndex, transaction);
            Thread.yield();
        } else {

            System.out.println(String.format("%d Passed checks, committing...", transaction.getTimestamp()));
            transaction.setAborted(false);


            for (Writehandle writehandle : transaction.getWritehandles()) {

                System.out.println(String.format("t%d Updating committed...", transaction.getTimestamp()));
                committed.put(writehandle.key, transaction.getTimestamp(), transaction.getTimestamp());
                System.out.println(String.format("t%d Wrote committed", transaction.getTimestamp()));
                System.out.println(String.format("%d t%d write %s %d", System.nanoTime(), transaction.getTimestamp(), writehandle.key, database.get(reader, writehandle.key).get(reader, transaction.getTimestamp()).value));
                if (rts.get(reader, writehandle.key) == transaction) {
                    rts.remove(threadIndex, writehandle.key);
                }
                touched.get(reader, writehandle.key).clear();

                ReentrantReadWriteLock.WriteLock lock = transaction.getLock(threadIndex, writehandle.key);

                lock.unlock();

            }
            lastCommit = transaction.getTimestamp();

            System.out.println(String.format("%d t%d won committed", System.nanoTime(), transaction.getTimestamp()));
            transaction.setTimestamp(Integer.MAX_VALUE);


        }

    }

    public void abort(int threadIndex, MegaTree.Transaction<V> transaction) {

        transaction.setAborted(true);


        for (MegaTree.Writehandle writehandle : transaction.getWritehandles()) {
            database.get(new LRHashMap.Reader(threadIndex), writehandle.key).remove(threadIndex, transaction.getTimestamp());
            if (rts.get(new LRHashMap.Reader(threadIndex), writehandle.key) == transaction) {
                rts.remove(threadIndex, writehandle.key);
            }
            List<MegaTree.Transaction> transactions = touched.get(new LRHashMap.Reader(threadIndex), writehandle.key);
            transactions.remove(transaction);
            ReentrantReadWriteLock.WriteLock lock = transaction.getLock(threadIndex, writehandle.key);

            lock.unlock();


        }
        transaction.clear(threadIndex, this);


    }

    public synchronized int issue(int threadIndex, MegaTree.Transaction transaction) {

        int token = this.counter;
        this.counter = this.counter + 1;
        abort(threadIndex, transaction);
        return token;

    }

    class Version<V> {
        public final int minVersion;
        public final int maxVersion;
        public final V value;

        public Version(int minVersion, int maxVersion, V value) {
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
            this.value = value;
        }
    }

    class Writehandle {
        String key;
        public Integer timestamp;

        public Writehandle(String key, Integer timestamp) {
            this.key = key;
            this.timestamp = timestamp;
        }


    }

    public interface Transaction<V> {
        List<MegaTree<V>.Writehandle> getWritehandles();

        int getTimestamp();

        void setAborted(boolean aborted);

        void clear(int threadIndex, MegaTree MegaTree);

        void addWrite(MegaTree.Writehandle writehandle);

        boolean getAborted();

        void setTimestamp(int timestamp);

        void cancel();

        boolean getCancelled();

        void addChallenger(MegaTree.Transaction transaction);

        List<MegaTree.Transaction> getChallengers();

        void addRead(MegaTree.Read readHandle);

        List<MegaTree.Read> getReadHandles();

        boolean checkChallengers(MegaTree MegaTree, MegaTree.Transaction transaction);

        void markPrecommit();

        boolean getPrecommit();

        void markRestart(boolean restart);

        boolean getRestart();

        int getNumberOfAttempts();

        void markSuccessful();

        boolean getSuccessful();

        void addLock(int threadIndex, String key, ReentrantReadWriteLock.WriteLock lock);

        ReentrantReadWriteLock.WriteLock getLock(int threadIndex, String key);


    }

    public class Read<V> {
        V value;
        Integer timestamp;


        public Read(V value, Integer timestamp) {
            this.value = value;
            this.timestamp = timestamp;

        }


    }

    public boolean shouldRestart(MegaTree.Transaction transaction, MegaTree.Transaction peek) {
        boolean defeated = (((peek.getTimestamp() < transaction.getTimestamp() ||
                (transaction.getNumberOfAttempts() < peek.getNumberOfAttempts())) && peek.getPrecommit()) ||
                peek.getPrecommit() && (peek.getTimestamp() > transaction.getTimestamp() ||
                        (peek.getNumberOfAttempts() > transaction.getNumberOfAttempts() && peek.getPrecommit())
                                && !peek.getRestart()));

        return defeated;
    }

    public void dump(int threadIndex) {
        System.out.println(database.get(new LRHashMap.Reader(threadIndex), "a").toString(threadIndex));
        System.out.println(committed.toString(threadIndex));
        System.out.println(lastCommit);
    }


    public Integer getLatest(int threadIndex, String key) {

        LRHashMap.Reader reader = new LRHashMap.Reader(threadIndex);
        return (Integer) database.get(reader, key).get(reader, committed.get(reader, key)).value;
    }

    public Integer getHighest(int threadIndex, String key) {
        LRHashMap.Reader reader = new LRHashMap.Reader(threadIndex);
        ArrayList<Integer> versions = new ArrayList<>(database.get(reader, key).keySet(reader));
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
        V value = database.get(reader, key).get(reader, versions.get(0)).value;
        return (Integer) value;
    }

    public boolean versionsInOrder(int threadIndex, String key) {
        LRHashMap.Reader reader = new LRHashMap.Reader(threadIndex);
        LRHashMap<Integer, MegaTree<V>.Version<V>> versionDb = database.get(reader, key);
        ArrayList<Integer> versions = new ArrayList<>(versionDb.keySet(reader));
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        });
        int previous = (Integer) versionDb.get(reader, versions.get(0)).value;
        System.out.println(previous);
        for (int i = 1; i < versions.size(); i++) {
            Integer version = versions.get(i);

            int difference = (Integer) versionDb.get(reader, version).value - previous;
            if (difference > 1) {
                System.out.println(versionDb.get(reader, version));
                System.out.println(difference);
                return false;
            }
            previous = (Integer) versionDb.get(reader, version).value;
        }
        return true;
    }

    public void printDuplicates(int threadIndex, String key) {
        LRHashMap.Reader reader = new LRHashMap.Reader(threadIndex);

        HashSet<Integer> found = new HashSet<>();
        for (Integer version : database.get(reader, key).keySet(reader)) {
            Integer value = (Integer) database.get(reader, key).get(reader, version).value;
            if (!found.add(value)) {
                System.out.println(String.format("%s %d is duplicated %d ", key, value, version));
            }
        }
    }

    public Integer getHighestVersion(int threadIndex, String key) {
        LRHashMap.Reader reader = new LRHashMap.Reader(threadIndex);

        ArrayList<Integer> list = new ArrayList<>(database.get(reader, key).keySet(reader));
        list.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
        return list.get(0);
    }
}
