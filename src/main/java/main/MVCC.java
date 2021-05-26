package main;

import java.util.*;

public class MVCC {

    private final HashMap<String, HashMap<Integer, Integer>> database;
    private final HashMap<String, Integer> committed;
    private int counter;

    public MVCC() {
        this.database = new HashMap<>();
        this.committed = new HashMap<>();

        this.counter = 0;
    }

    public synchronized int issue() {
        int token = this.counter;
        this.counter = this.counter + 1;
        return token;
    }

    public void dump() {
        System.out.println(database);
        System.out.println(committed);
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
    }

    public Writehandle intend_to_write(Transaction transaction, String key, Integer value) {
        if (transaction.getTimestamp() < timestamp_of_key(transaction, key)) {
            return null;
        }
        if (!database.containsKey(key)) {
            database.put(key, new HashMap<>());
        }
        database.get(key).put(transaction.getTimestamp(), value);
        return new Writehandle(key);
    }

    private int timestamp_of_key(Transaction transaction, String key) {
        if (!database.containsKey(key)) {
            return transaction.getTimestamp();
        }

        String latest_key = String.format("%s", key);
        if (committed.containsKey(latest_key)) {
            return committed.get(latest_key);
        } else {
            return transaction.getTimestamp();
        }

    }

    public Integer read(String key, Transaction transaction) {
        HashMap<Integer, Integer> values = database.get(key);
        ArrayList<Integer> versions = new ArrayList<>(values.keySet());
        versions.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });

        for (Integer version : versions) {
            if (version <= transaction.getTimestamp()) {
                return values.get(version);
            }
        }
        return null;
    }

    public void commit(Transaction transaction) {
        boolean restart = false;
        for (Writehandle writehandle : transaction.getWritehandles()) {
            if (transaction.getTimestamp() < timestamp_of_key(transaction, writehandle.key)) {
                restart = true;
            }
        }
        if (restart) {
            transaction.setAborted(true);
        } else {
            transaction.setAborted(false);
            for (Writehandle writehandle : transaction.getWritehandles()) {
                committed.put(writehandle.key, transaction.getTimestamp());
            }
        }
    }




}
