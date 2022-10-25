package main;

import java.util.*;

import static java.lang.Math.floor;
import static java.lang.Math.max;

public class BankSynchronizer2 extends Thread {
    private final int transactionCount;
    private int[] lastInsert;
    public HashSet<Transaction> allTransactions;
    private HashSet<Transaction> waitingDispatch;
    private static final int NEITHER = -1;
    private Map<Transaction, Integer> pending;
    private final int id;
    private final List<BankSynchronizer2.Account> accounts;
    private int accountBucket;
    private int[] writing;
    private int size;
    public Transaction[][] queued;
    private boolean running = true;
    private int requests;
    private ArrayList<BankSynchronizer2> threads;
    private int buckets;
    private boolean synchronizer = false;
    private ArrayList<BankSynchronizer2> synchronizerThreads;
    private double bucketSize;
    private int synchronizerSize;
    private int threadsSize;

    public BankSynchronizer2(int transactionCount, int size, int bucket, int bucketSize, boolean synchronizer, int id, List<BankSynchronizer2.Account> accounts) {
        this.transactionCount = transactionCount;
        this.size = size;
        this.buckets = bucket;
        this.synchronizer = synchronizer;
        this.id = id;
        this.accounts = accounts;
        this.bucketSize = bucketSize;
        this.waitingDispatch = new HashSet<>();
        this.pending = new HashMap<>();
        this.allTransactions = new HashSet<>();
    }

    private static class Account {
        public Integer id;
        private int balance;

        public Account(int id) {
            this.id = id;
            this.balance = new Random().nextInt(2500);
        }

    }

    public static void main(String[] args) throws InterruptedException {
        int accountCount = 50;
        int threadCount = 9;
        int transactionCount = 5000;
        System.out.println(String.format("Using %d transaction generator threads.", threadCount));
        int size = 9999999;
        System.out.println(String.format("Each transaction processor thread has a buffer of %d items", size));
        int synchronizerThreads = 3;
        System.out.println(String.format("Transaction processor threads: %d", synchronizerThreads));
        int bucketSize = (accountCount + 1) / synchronizerThreads;
        System.out.println(String.format("Each transaction processor thread handles %d accounts in a bucket", bucketSize));
        List<BankSynchronizer2.Account> accounts = new ArrayList<>();
        int id = 0;
        int money = 0;
        for (int i = 0; i < accountCount; i++) {
            // System.out.println(String.format("Bucket %d", (int) floor(i / bucketSize)));
            accounts.add(new BankSynchronizer2.Account(id));
            id++;
        }
        System.out.println(String.format("Created %d accounts with random balances", accountCount));
        for (BankSynchronizer2.Account account : accounts) {
            money += account.balance;
        }
        System.out.println(String.format("= Total money = %d", money));
        int threadId = 0;

        List<BankSynchronizer2> regularThreads = new ArrayList<>();
        List<BankSynchronizer2> threads = new ArrayList<>();
        List<BankSynchronizer2> allSynchronizerThreads = new ArrayList<>();
        int bucket = 0;
        for (int i = 0; i < synchronizerThreads; i++) {
            BankSynchronizer2 synchronizerThread = new BankSynchronizer2(transactionCount, size, synchronizerThreads, bucketSize, true, threadId++, accounts);
            threads.add(synchronizerThread);
            allSynchronizerThreads.add(synchronizerThread);
        }
        for (int i = 0; i < threadCount; i++) {
            BankSynchronizer2 regularThread = new BankSynchronizer2(transactionCount, size, synchronizerThreads, bucketSize, false, threadId++, accounts);
            threads.add(regularThread);
            regularThreads.add(regularThread);

        }
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).setThreads(new ArrayList<BankSynchronizer2>(allSynchronizerThreads), new ArrayList<>(threads));
        }

        System.out.println(String.format("Started %d transaction generator threads %d transaction processor threads", threadCount, synchronizerThreads));
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        System.out.println("Beginning test");
        Thread.sleep(5000);

        for (int i = 0; i < regularThreads.size(); i++) {
            regularThreads.get(i).running = false;
        }
        for (int i = 0; i < regularThreads.size(); i++) {
            regularThreads.get(i).join();
        }
        System.out.println("Transaction test complete");

        for (int i = 0; i < allSynchronizerThreads.size(); i++) {
            allSynchronizerThreads.get(i).running = false;
        }
        for (int i = 0; i < allSynchronizerThreads.size(); i++) {
            allSynchronizerThreads.get(i).join();
        }
        System.out.println("Synchronizer threads test finished");

        long end = System.currentTimeMillis();

        int totalMoney = 0;
        for (BankSynchronizer2.Account account : accounts) {
            totalMoney += account.balance;
        }

        for (BankSynchronizer2 thread : threads) {
            for (Transaction transaction : thread.allTransactions) {
                assert transaction.executionCount == 1 &&
                        transaction.done: String.format("%d %b",
                        transaction.executionCount, transaction.done);
            }
        }

        assert totalMoney == money : totalMoney;
        System.out.println(String.format("= Total Money = %d", totalMoney));
        int totalRequests = 0;
        for (BankSynchronizer2 thread : threads) {
            totalRequests += thread.requests;
        }
        double seconds = (end - start) / 1000.0;
        System.out.println("Finished");
        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Took %f seconds", seconds));
    }

    private void setThreads(ArrayList<BankSynchronizer2> synchronizerThreads, ArrayList<BankSynchronizer2> threads) {
        this.threads = threads;
        this.synchronizerThreads = synchronizerThreads;
        this.synchronizerSize = synchronizerThreads.size();
        this.threadsSize = threads.size();
        this.writing = new int[threadsSize];
        if (synchronizer) {
            this.queued = new Transaction[(int) bucketSize + 2][size];
        }
        this.lastInsert = new int[(int) bucketSize + 2];
        Arrays.fill(this.lastInsert, 0);
        Arrays.fill(this.writing, NEITHER);

    }

    public void run() {
        List<Transaction> removals = new ArrayList<>();
        List<Transaction> successes = new ArrayList<>();

        Random rng = new Random();

        if (synchronizer) {
            boolean allEmpty = true;
            while (running || !allEmpty) {
                allEmpty = true;
                // System.out.println(String.format("%d Processing transactions", id));
                int nullcount = 0;

                for (int b = 0; b < queued.length; b++) {
                    boolean cancel = false;

                    for (int j = 0; j < queued[b].length; j++) {

                        Transaction transaction = queued[b][j];
                        if (transaction != null && !transaction.done) {
                            allEmpty = false;
                            // queued[b][j] = null;
                            nullcount = 0;
                            transaction.done = true;
                            transaction.executionCount++;
                            accounts.get(transaction.account).balance += transaction.amount;


                        }
                        if (transaction == null || transaction.done) {
                            nullcount++;
                            if (nullcount > 2500) {
                                  cancel = true;
                            }

                        }
                        if (cancel) {
                            break;
                        }


                    }
                    lastInsert[b] = 0;

                }

                if (nullcount == (int) (size * bucketSize)) {
                    // System.out.println("No new records");
                    allEmpty = true;
                }

                // Thread.yield();
            }

        }
//        System.out.println(String.format("Starting worker thread %b", synchronizer));
        if (!synchronizer) {

            while (running || waitingDispatch.size() > 0 || pending.size() > 0) {
//                System.out.println(String.format("%d %d %d", id, waitingDispatch.size(), pending.size()));
                // detect race
                for (Map.Entry<Transaction, Integer> transaction : pending.entrySet()) {
                    int synchronizerThread = max(0, (int) floor((transaction.getKey().account / bucketSize)) - 1);
                    int bucket = transaction.getKey().account % (int) floor(bucketSize);
                    // System.out.println(String.format("%d %d %d", transaction.getKey().account, synchronizerThread, bucket));
                    Transaction found = synchronizerThreads.get(synchronizerThread).queued[bucket][transaction.getValue()];

                    transaction.getKey().failCount++;

                    if ((found == null || found != transaction.getKey()) && transaction.getKey().done == false) {
                        if (!this.dispatch(transaction.getKey())) {
                            System.out.println("Failed to enqueue");
                        } else {
                            // System.out.println("Successful enqueue");
                        }
//                        System.out.println("Data race");
                    } else if (transaction.getKey().done) {
                        removals.add(transaction.getKey());
                    }
                }
                for (Transaction removal : removals) {
                    pending.remove(removal);
                    waitingDispatch.remove(removal);
                }
                removals.clear();
                for (Transaction transaction : waitingDispatch) {
                    if (this.dispatch(transaction)) {
                        successes.add(transaction);
                    }
                }
                for (Transaction success : successes) {
                    waitingDispatch.remove(success);
                }

                if (!running) {
                    continue;
                }
                for (int i = 0 ; i < transactionCount; i++) {
                    int amount = rng.nextInt(1000);
                    int sourceAccount = 0;
                    int destinationAccount = 0;
                    while (sourceAccount == destinationAccount) {
                        sourceAccount = rng.nextInt(accounts.size());
                        destinationAccount = rng.nextInt(accounts.size());
                        if (sourceAccount == destinationAccount) {
                            destinationAccount = (sourceAccount + 1) % accounts.size();
                        }
                    }
                    Transaction out = new Transaction(id, sourceAccount, -amount);
                    if (!this.dispatch(out)) {
                        waitingDispatch.add(out);
                    }
                    Transaction in = new Transaction(id, destinationAccount, amount);
                    if (!this.dispatch(in)) {
                        waitingDispatch.add(in);
                    }
                    requests++;
                }
                // Thread.yield();
            }

        }
    }

    private boolean dispatch(Transaction transaction) {
        int account = transaction.account;
        int synchronizerThread = max(0, (int) floor((account / bucketSize)) - 1);
        int bucket = (int) (account % bucketSize);
        boolean inserted = false;

        if (transaction.done) {
            return true;
        }

        allTransactions.add(transaction);
        // System.out.println(String.format("Dispatching %d", id));
        for (int i = 0; i < synchronizerThreads.get(synchronizerThread).queued[bucket].length; i++) {
            Transaction[] transactions = synchronizerThreads.get(synchronizerThread).queued[bucket];
            Transaction replacement = transactions[i];
            if (replacement == null || replacement.done) {
//                System.out.println(i);
                inserted = true;
                transactions[i] = transaction;
                pending.put(transaction, i);
                // synchronizerThreads.get(synchronizerThread).lastInsert[bucket] = i + 1;
                break;
            }
        }
        if (synchronizerThreads.get(synchronizerThread).lastInsert[bucket] == size) {
            synchronizerThreads.get(synchronizerThread).lastInsert[bucket] = 0;
        }
        // System.out.println("Failed to insert");
        return inserted;
    }

    private class Transaction {
        public final int account;
        private final int amount;
        public volatile boolean done;
        public int executionCount;
        public int failCount;
        private int owner;

        public Transaction(int owner, int sourceAccount, int amount) {
            this.owner = owner;
            this.done = false;
            this.account = sourceAccount;
            this.amount = amount;
        }
    }
}
