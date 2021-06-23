package main;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import static java.lang.Integer.max;


public class MVCC {

    boolean ready;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> database;
    private ConcurrentHashMap<String, Integer> committed;
    private volatile int counter;
    private boolean earlyAborts;
    private volatile int lastCommit;
//    private ConcurrentHashMap<String, Transaction> rts;
    private ConcurrentHashMap<String, Integer> wts;
    private ConcurrentHashMap<String, Transaction> rts;




    public MVCC(boolean earlyAborts) {
        this.earlyAborts = earlyAborts;
        this.database = new ConcurrentHashMap<>();
        this.committed = new ConcurrentHashMap<>();

        this.counter = 0;
        this.lastCommit = 0;
//        this.rts = new ConcurrentHashMap<String, Transaction>();
        this.wts = new ConcurrentHashMap<String, Integer>();
        this.rts = new ConcurrentHashMap<>();
    }

    public synchronized int issue(Transaction transaction) {

        int token = this.counter;
        this.counter = this.counter + 1;

        return token;

    }

    public void dump() {
        System.out.println(database);

    }

    public void ensure_keys(String... keys) {
        for (String key : keys) {
            System.out.println(String.format("%s doesn't exist, creating", key));
            ConcurrentHashMap<Integer, Integer> newdata = new ConcurrentHashMap<>();
            database.put(key, newdata);


        }
    }

    public Integer getLatest(String key) {
        return database.get(key).get(committed.get(key));
    }

    public Integer getHighest(String key) {
        ArrayList<Integer> versions = new ArrayList<>(database.get(key).keySet());
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
        return database.get(key).get(versions.get(0));
    }

    public boolean versionsInOrder(String key) {
        ConcurrentHashMap<Integer, Integer> versionDb = database.get(key);
        ArrayList<Integer> versions = Collections.list(versionDb.keys());
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        });
        int previous = versionDb.get(versions.get(0));
        System.out.println(previous);
        for (int i = 1; i < versions.size(); i++) {
            Integer version = versions.get(i);

            int difference = versionDb.get(version) - previous;
            if (difference > 1) {
                System.out.println(versionDb.get(version));
                System.out.println(difference);
                return false;
            }
            previous = versionDb.get(version);
        }
        return true;
    }

    class Writehandle {
        String key;
        public Integer timestamp;

        public Writehandle(String key, Integer timestamp) {
            this.key = key;
            this.timestamp = timestamp;
        }


    }

    public interface Transaction {
        List<Writehandle> getWritehandles();

        int getTimestamp();

        void setAborted(boolean aborted);

        void clear();

        void addWrite(Writehandle writehandle);

        boolean getAborted();

        void setTimestamp(int timestamp);

        void cancel();

        boolean getCancelled();
    }

    public Writehandle intend_to_write(Transaction transaction, String key, Integer value, Integer timestamp) {
        String conflictType = "";
        boolean restart = false;
        if (earlyAborts) {
            Transaction peek = rts.get(key);

            if (peek != null && peek.getTimestamp() < transaction.getTimestamp()) {
                System.out.println(String.format("%d wins against %d", peek, transaction.getTimestamp()));
                restart = true;
            }

            if (restart) {
                System.out.println(String.format("%d %s failed - early abort", transaction.getTimestamp(), conflictType));
                return null;
            }
        }

        database.get(key).put(transaction.getTimestamp(), value);
        wts.put(key, transaction.getTimestamp());

        Writehandle writehandle = new Writehandle(key, timestamp);
        transaction.addWrite(writehandle);
        return writehandle;
    }

    public Read read(Transaction transaction, String key) {
        ConcurrentHashMap<Integer, Integer> values = database.get(key);

        // System.out.println(String.format("%d Values in database for key %s %s  committed %d", transaction.getTimestamp(), key, values, committed.get(key)));
        ArrayList<Integer> versions = new ArrayList<>(values.keySet());
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });

        // read your own writes
        if (values.containsKey(transaction.getTimestamp())) {
            return new Read(values.get(transaction.getTimestamp()), transaction.getTimestamp());
        }
        for (Integer version : versions) {
            System.out.println(String.format("%d Inspecting version %d", transaction.getTimestamp(), version));
            if (version <= transaction.getTimestamp()) {

                Integer lastKnownCommit = committed.get(key);
                if (version >= lastCommit && version.equals(lastKnownCommit)) {
                    Integer read = values.get(version);


                    Transaction peek = rts.get(key);

                    if (peek != null && transaction.getTimestamp() > peek.getTimestamp()) {
                        System.out.println(String.format("%d %d should win %b", transaction.getTimestamp(), peek.getTimestamp(), peek.getAborted()));

                        return null;
                    }


                    rts.put(key, transaction);
                    int peekTimestamp = 0;
                    if (peek != null) {
                        peekTimestamp = peek.getTimestamp();
                    }

                    System.out.println(String.format("%d %d read %s %d %d", System.nanoTime(), transaction.getTimestamp(), key, read, peekTimestamp));
                    if (read == null) {
                        System.out.println("ERROR");
                        throw new IllegalArgumentException();
                    }
                    return new Read(read, lastKnownCommit);
                } else {
                    System.out.println(String.format("%d %d Version is not equal to committed %d %d", transaction.getTimestamp(), version, lastKnownCommit, lastCommit));
                }

            } else {
                // System.out.println("Ignoring newer value");
            }
        }
        System.out.println(String.format("%d No versions valid", transaction.getTimestamp()));
        return null;
    }

    public class Read {
        Integer value;
        Integer timestamp;

        public Read(Integer value, Integer timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    public void commit(Transaction transaction) {


        boolean restart = false;
        String conflictType = "";
        if (!ready) {
            restart = true;
        }
        ready = false;
        if (transaction.getTimestamp() < lastCommit) {
            restart = true;
        }

        for (Writehandle writehandle : transaction.getWritehandles()) {
            Transaction peek = rts.get(writehandle.key);
            int peakTimestamp = 0;
            if (peek != null) {
                peakTimestamp = peek.getTimestamp();
            }
            System.out.println(String.format("%d %d begin commit (peek=%d)", System.nanoTime(), transaction.getTimestamp(), peakTimestamp));
            if (peek != null && peek.getTimestamp() < transaction.getTimestamp()) {
                System.out.println(String.format("%d wins against %d", peek.getTimestamp(), transaction.getTimestamp()));
                restart = true;
                conflictType = "read";
                break;
            }


            if (wts.containsKey(writehandle.key) && wts.get(writehandle.key) < transaction.getTimestamp()) {
                restart = true;
                conflictType = "someonewrote";
                break;
            }

            if (committed.containsKey(writehandle.key) && writehandle.timestamp != null && !committed.get(writehandle.key).equals(writehandle.timestamp)) {
                restart = true;
                conflictType = "commit";
                break;
            }


        }

        if (restart) {

            System.out.println(String.format("%d %s Conflict. Restarting transaction", transaction.getTimestamp(), conflictType));
            transaction.setAborted(true);

            for (Writehandle writehandle : transaction.getWritehandles()) {
                database.get(writehandle.key).remove(transaction.getTimestamp());

            }
            transaction.clear();
            ready = true;
        } else {
            transaction.setAborted(false);


            for (Writehandle writehandle : transaction.getWritehandles()) {
//                Integer integer = committed.get(writehandle.key);
//                if (integer == null)  {
//                    integer = transaction.getTimestamp();
//                }
                committed.put(writehandle.key, transaction.getTimestamp());
                System.out.println(String.format("%d %d write %s %d", System.nanoTime(), transaction.getTimestamp(), writehandle.key, database.get(writehandle.key).get(transaction.getTimestamp())));

            }
            lastCommit = transaction.getTimestamp();
            System.out.println(String.format("%d %d won committed", System.nanoTime(), transaction.getTimestamp()));
            transaction.setTimestamp(Integer.MAX_VALUE);
            ready = true;
        }

    }

    public void printDuplicates(String key) {
        HashSet<Integer> found = new HashSet<>();
        for (Integer version : database.get(key).keySet()) {
            Integer value = database.get(key).get(version);
            if (!found.add(value)) {
                System.out.println(String.format("%s %d is duplicated %d ", key, value, version));
            }
        }
    }

    public Integer getHighestVersion(String key) {
        ArrayList<Integer> list = Collections.list(database.get(key).keys());
        list.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
        return list.get(0);
    }
}
