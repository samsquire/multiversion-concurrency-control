package main;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Integer.max;
import static java.lang.Integer.min;


public class MVCC {


    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> database;
    private ConcurrentHashMap<String, Integer> committed;
    private volatile int counter;
    private boolean earlyAborts;
    private volatile int lastCommit;
    //    private ConcurrentHashMap<String, Transaction> rts;
    private ConcurrentHashMap<String, Integer> wts;
    private ConcurrentHashMap<String, Transaction> rts;
    private int precommit;
    private List<Transaction> active;
    private ConcurrentHashMap<String, String> locks;
    private ConcurrentHashMap<String, List<Transaction>> touched;


    public MVCC(boolean earlyAborts) {
        this.earlyAborts = earlyAborts;
        this.database = new ConcurrentHashMap<>();
        this.committed = new ConcurrentHashMap<>();
        this.active = Collections.synchronizedList(new ArrayList<>());
        this.counter = 0;
        this.lastCommit = 0;
        this.wts = new ConcurrentHashMap<String, Integer>();
        this.rts = new ConcurrentHashMap<>();
        this.locks = new ConcurrentHashMap<>();
        this.touched = new ConcurrentHashMap<>();
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
            touched.put(key, Collections.synchronizedList(new ArrayList<Transaction>()));

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

    public void validate(Transaction transaction) {
        String lockKey = transaction.createLockKey();
        if (!locks.containsKey(lockKey)) {
            locks.put(lockKey, "locked");
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

        void addChallenger(Transaction transaction);

        List<Transaction> getChallengers();

        void addRead(Read readHandle);

        List<Read> getReadHandles();

        boolean checkChallengers(Transaction transaction);

        String createLockKey();

        void markPrecommit();

        boolean getPrecommit();

        void markRestart(boolean restart);

        boolean getRestart();
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
                    if (peek != null) {
                        peek.addChallenger(transaction);
                    }


                    rts.put(key, transaction);
                    touched.get(key).add(transaction);
                    int peekTimestamp = 0;
                    if (peek != null) {
                        peekTimestamp = peek.getTimestamp();
                    }

                    System.out.println(String.format("%d %d read %s %d %d", System.nanoTime(), transaction.getTimestamp(), key, read, peekTimestamp));
                    if (read == null) {
                        System.out.println("ERROR");
                        throw new IllegalArgumentException();
                    }
                    Read readHandle = new Read(read, lastKnownCommit);
                    transaction.addRead(readHandle);
                    return readHandle;
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
        transaction.markPrecommit();


//        synchronized (locks.get(transaction.createLockKey())) {
        boolean restart = false;
        String conflictType = "";

        if (transaction.getTimestamp() < lastCommit) {
            transaction.markRestart(true);
            conflictType = "ahead";
        }


        for (Writehandle writehandle : transaction.getWritehandles()) {
            Transaction peek = rts.get(writehandle.key);
            int peakTimestamp = 0;
            if (peek != null) {
                peakTimestamp = peek.getTimestamp();
            }
            System.out.println(String.format("%d %d begin commit (peek=%d)", System.nanoTime(), transaction.getTimestamp(), peakTimestamp));
            if (peek != null && peek.getTimestamp() != transaction.getTimestamp()) {
                System.out.println(String.format("%d wins against %d", peek.getTimestamp(), transaction.getTimestamp()));
                transaction.markRestart(true);
                conflictType = "read";
                break;
            }

            if (touched.containsKey(writehandle.key)) {
                List<Transaction> transactions = touched.get(writehandle.key);
                synchronized (transactions) {
                    for (Transaction other : transactions) {
                        if (other.getTimestamp() < transaction.getTimestamp() || other.getPrecommit() && other.getTimestamp() > transaction.getTimestamp() && !other.getRestart()) {
                            transaction.markRestart(true);
                            break;
                        }
                    }
                }
            }

            if (committed.containsKey(writehandle.key) && writehandle.timestamp != null && !committed.get(writehandle.key).equals(writehandle.timestamp)) {
                transaction.markRestart(true);
                conflictType = "commit";
                break;
            }


        }



        List<Transaction> challengers = transaction.getChallengers();
        System.out.println(String.format("%d Checking challengers %d", transaction.getTimestamp(), challengers.size()));

        if (transaction.checkChallengers(transaction)) {
            transaction.markRestart(true);
            conflictType = "challenger";
        }

        System.out.println(String.format("%d Challengers checked", transaction.getTimestamp()));


        precommit = max(transaction.getTimestamp(), precommit);




        if (transaction.getRestart()) {

            System.out.println(String.format("%d Conflict %s Restarting transaction", transaction.getTimestamp(), conflictType));
            transaction.setAborted(true);

            for (Writehandle writehandle : transaction.getWritehandles()) {
                database.get(writehandle.key).remove(transaction.getTimestamp());
                if (rts.get(writehandle.key) == transaction) {
                    rts.remove(writehandle.key);
                }
                List<Transaction> transactions = touched.get(writehandle.key);
                synchronized (transactions) {
                    transactions.remove(transaction);
                }
            }
            transaction.clear();


        } else {

            System.out.println(String.format("%d Passed checks, committing...", transaction.getTimestamp()));
            transaction.setAborted(false);


            for (Writehandle writehandle : transaction.getWritehandles()) {


                committed.put(writehandle.key, transaction.getTimestamp());
                System.out.println(String.format("%d %d write %s %d", System.nanoTime(), transaction.getTimestamp(), writehandle.key, database.get(writehandle.key).get(transaction.getTimestamp())));
                if (rts.get(writehandle.key) == transaction) {
                    rts.remove(writehandle.key);
                }
                touched.get(writehandle.key).clear();
            }
            lastCommit = transaction.getTimestamp();

            System.out.println(String.format("%d %d won committed", System.nanoTime(), transaction.getTimestamp()));
            transaction.setTimestamp(Integer.MAX_VALUE);


        }
//        }

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
