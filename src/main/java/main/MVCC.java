package main;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MVCC {

    private final ConcurrentHashMap<String, Integer> rts;
    private final ConcurrentHashMap<String, Integer> wts;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> database;
    private ConcurrentHashMap<String, Integer> committed;
    private int counter;
    private boolean earlyAborts;
    private int lastCommit;
    private Set<Transaction> transactions;


    public MVCC(boolean earlyAborts) {
        this.earlyAborts = earlyAborts;
        this.database = new ConcurrentHashMap<>();
        this.committed = new ConcurrentHashMap<>();
        this.rts = new ConcurrentHashMap<>();
        this.wts = new ConcurrentHashMap<>();
        this.transactions = new HashSet<>();
        this.counter = 0;
        this.lastCommit = 0;
    }

    public int issue(Transaction transaction) {
        synchronized (database) {
            int token = this.counter;
            this.counter = this.counter + 1;
            this.transactions.add(transaction);

            return token;
        }
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

    }

    public Writehandle intend_to_write(Transaction transaction, String key, Integer value, Integer timestamp) {
        String conflictType = "";
        boolean restart = false;
        if (earlyAborts) {
            if (rts.containsKey(key) && transaction.getTimestamp() < rts.get(key)) {
                restart = true;
                conflictType = "read";
            }

            if (restart) {
                System.out.println(String.format("%d %s failed - early abort", transaction.getTimestamp(), conflictType));
                return null;
            }
        }
//        if (committed.containsKey(key) && lastCommit > committed.get(key)) {
//            System.out.println("Someone beat us 2");
//            return null;
//        }


        database.get(key).put(transaction.getTimestamp(), value);


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
            if (version <= transaction.getTimestamp()) {
                if (version <= lastCommit) {
                    Integer timestamp = committed.get(key);
                    if (version.equals(timestamp)) {
                        Integer read = values.get(version);

                        if (rts.containsKey(key) && rts.get(key) > transaction.getTimestamp()) {
                            System.out.println(String.format("%d RTS ahead", transaction.getTimestamp()));
                            return null;
                        }

                        if (lastCommit > timestamp) {
                            System.out.println("Race ahead");
                            return null;
                        }
                        rts.put(key, transaction.getTimestamp());

                        System.out.println(String.format("%d %s read %d", transaction.getTimestamp(), key, read));
                        if (read == null) {
                            System.out.println("ERROR");
                        }
                        return new Read(read, timestamp);
                    }
                }
            }
        }

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
        transactions.remove(transaction);
        System.out.println(String.format("%d %d begin commit", System.nanoTime(), transaction.getTimestamp()));

        boolean restart = false;
        String conflictType = "";
//        for (Transaction other : new ArrayList<>(transactions)) {
//            if (transaction == other) {
//                continue;
//            }
//            for (Writehandle writehandle : transaction.getWritehandles()) {
//                if (transaction.getRts(writehandle.key) > other.getRts(writehandle.key)) {
//                    restart = true;
//                    // let the younger transaction complete
//                    conflictType = "read";
//                    break;
//                }
//            }
//            if (restart) {
//                break;
//            }
//
//
//
//        }
        for (Writehandle writehandle : transaction.getWritehandles()) {
            if (rts.containsKey(writehandle.key) && rts.get(writehandle.key) > transaction.getTimestamp()) {
                restart = true;
                conflictType = "read";
            }
            if (committed.containsKey(writehandle.key) && writehandle.timestamp != null && !committed.get(writehandle.key).equals(writehandle.timestamp)) {
                restart = true;
                conflictType = "commit";
            }
        }

        if (restart) {
            transactions.add(transaction);
            System.out.println(String.format("%d %s Conflict. Restarting transaction", transaction.getTimestamp(), conflictType));
            transaction.setAborted(true);

            for (Writehandle writehandle : transaction.getWritehandles()) {
                database.get(writehandle.key).remove(transaction.getTimestamp());
            }
            transaction.clear();
        } else {

            transaction.setAborted(false);


            for (Writehandle writehandle : transaction.getWritehandles()) {
                committed.put(writehandle.key, transaction.getTimestamp());
                System.out.println(String.format("%d %d write %s %d", System.nanoTime(), transaction.getTimestamp(), writehandle.key, database.get(writehandle.key).get(transaction.getTimestamp())));
                wts.put(writehandle.key, transaction.getTimestamp());
            }
            lastCommit = transaction.getTimestamp();
            System.out.println(String.format("%d won committed", transaction.getTimestamp()));
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
