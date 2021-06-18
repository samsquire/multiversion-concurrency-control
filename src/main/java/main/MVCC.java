package main;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MVCC {

    private final ConcurrentHashMap<String, Integer> rts;
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Integer>> database;
    private ConcurrentHashMap<String, Integer> committed;
    private int counter;
    private boolean earlyAborts;
    private int lastCommit;


    public MVCC(boolean earlyAborts) {
        this.database = new ConcurrentHashMap<>();
        this.committed = new ConcurrentHashMap<>();
        this.rts = new ConcurrentHashMap<>();
        this.counter = 0;
        this.lastCommit = 0;
    }

    public int issue() {
        synchronized (database) {
            int token = this.counter;
            this.counter = this.counter + 1;

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

    class Writehandle {
        String key;

        public Writehandle(String key) {
            this.key = key;
        }
    }

    public interface Transaction {
        List<Writehandle> getWritehandles();

        int getTimestamp();

        void setAborted(boolean aborted);

        void clear();

        void addWrite(Writehandle writehandle);
    }

    public Writehandle intend_to_write(Transaction transaction, String key, Integer value) {
        String conflictType = "";
        boolean restart = false;
        if (earlyAborts) {
            if (rts.containsKey(key) && rts.get(key) > transaction.getTimestamp()) {
                restart = true;
                conflictType = "read";
            }
            if (transaction.getTimestamp() < timestamp_of_key(transaction, key)) {
                restart = true;
                conflictType = "write";
            }
            if (restart) {
                System.out.println(String.format("%s failed - early abort", conflictType));
                return null;
            }
        }


        database.get(key).put(transaction.getTimestamp(), value);


        Writehandle writehandle = new Writehandle(key);
        transaction.addWrite(writehandle);
        return writehandle;
    }


    private int timestamp_of_key(Transaction transaction, String key) {
        if (!database.containsKey(key)) {
            return transaction.getTimestamp();
        }

        ArrayList<Integer> versions = new ArrayList<>(database.get(key).keySet());
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
        if (versions.size() > 0) {
            if (!committed.containsKey(key)) {
                return transaction.getTimestamp();
            } else {
                if (lastCommit > committed.get(key)) {
                    return lastCommit;
                } else {
                    return committed.get(key);
                }
            }

        } else {
            return transaction.getTimestamp();
        }

    }

    public Integer read(Transaction transaction, String key) {
        ConcurrentHashMap<Integer, Integer> values = database.get(key);
        System.out.println(String.format("%d Values in database %s for key %s committed %d", transaction.getTimestamp(), values, key, committed.get(key)));
        ArrayList<Integer> versions = new ArrayList<>(values.keySet());
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });

        if (values.containsKey(transaction.getTimestamp())) {
            return values.get(transaction.getTimestamp());
        }

        for (Integer version : versions) {
            if (version <= transaction.getTimestamp()) {
                if (version.equals(committed.get(key))) {
                    Integer read = values.get(version);

                    if (rts.containsKey(key) && rts.get(key) > transaction.getTimestamp()) {
                        System.out.println("Someone beat us");
                        return null;
                    }
                    rts.put(key, transaction.getTimestamp());
                    if (read == null) {
                        System.out.println("READ WAS NULL");
                    }
                    System.out.println(String.format("%d read %d", transaction.getTimestamp(), read));
                    return read;
                }
            }
        }

        return null;
    }

    public void commit(Transaction transaction) {

        boolean restart = false;
        String conflictType = "";
        for (Writehandle writehandle : transaction.getWritehandles()) {
            if (rts.containsKey(writehandle.key) && rts.get(writehandle.key) > transaction.getTimestamp()) {
                restart = true;
                conflictType = "read";
            }
//            if (transaction.getTimestamp() < timestamp_of_key(transaction, writehandle.key)) {
//                restart = true;
//                conflictType = "write";
//            }
        }

        if (restart) {
            System.out.println(String.format("%d %s Conflict. Restarting transaction", transaction.getTimestamp(), conflictType));
            transaction.setAborted(true);

            for (Writehandle writehandle : transaction.getWritehandles()) {
                database.get(writehandle.key).remove(transaction.getTimestamp());
            }
            transaction.clear();
        } else {
            transaction.setAborted(false);
            lastCommit = transaction.getTimestamp();
            for (Writehandle writehandle : transaction.getWritehandles()) {
                committed.put(writehandle.key, transaction.getTimestamp());
            }
        }
        System.out.println(String.format("%d committed", transaction.getTimestamp()));
    }


}
