package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ShardedBankNonRandom extends Thread {
    private final DoublyLinkedList data;
    private final ArrayList<Integer> data2;
    private final Account[] accounts;
    private final ShardedBankNonRandom seer;
    private int threadId;
    private final String type;
    private volatile boolean running = true;
    private long size;
    private int count;
    private int n = 1;
    private int multiple;
    boolean stopping;
    private ReentrantReadWriteLock transactionLock;
    private List<List<Transaction>> transactionsSeer;
    private List<List<Transaction>> transactions;
    private int threadCount;
    private List<ShardedBankNonRandom> threads;
    private int transactionsApplied = 0;
    private int get;
    private int MAX_BUFFER = 100;
    private int transactionsGenerated;

    public ShardedBankNonRandom(List<ShardedBankNonRandom> threads,
                                int threadId,
                                String type,
                                long size,
                                int multiple,
                                Account[] accounts,
                                ShardedBankNonRandom seer,
                                int threadCount) {
        this.threads = threads;
        this.threadId = threadId;
        this.type = type;
        this.size = size;
        this.multiple = multiple;
        this.threadCount = threadCount;
        this.data = new DoublyLinkedList(0, System.currentTimeMillis());
        this.data2 = new ArrayList<>();
        this.accounts = accounts;
        this.seer = seer;
        this.transactionLock = new ReentrantReadWriteLock();
        this.transactions = new ArrayList<>();
        this.transactionsSeer = new ArrayList<>();
    }

    public void run() {
        Random rng = new Random();
        int length = accounts.length;
        int sourceAccount = 0;
        int destinationAccount = 1;
        if (type.equals("transacter")) {
            while (running || stopping) {
                if (transactions.size() >= 1) {
                    this.transactionLock.writeLock().lock();
                    List<Transaction> remove = transactions.remove(0);
                    this.transactionLock.writeLock().unlock();
                    boolean removed = false;
                    if (remove != null) {
                        Transaction transaction = remove.get(0);


                        transactionsApplied++;
                        accounts[transaction.sourceAccount].balance -= transaction.amount;
                        accounts[transaction.destinationAccount].balance += transaction.amount;
                    } else {
                        System.out.println("Failed message");
                    }
                }
                if (stopping) { continue; }


                int amount = 0;
                Account source = accounts[sourceAccount];
                if (source.balance > 1) {

                    amount = rng.nextInt(source.balance);
                } else {
                    amount = 75;

                }
                transactionsGenerated++;
                // System.out.println(amount);

                if (source.balance >= amount) {

                    transactionsApplied++;
                    source.balance -= amount;
                    Account destination = accounts[destinationAccount];
                    destination.balance += amount;
                } else {
//                    System.out.println("not enough money in account");
//                     too much money to be spent
                    seer.queue(new Transaction(sourceAccount, destinationAccount, amount));
                }
                sourceAccount++;
                destinationAccount++;
                sourceAccount = sourceAccount % length;
                destinationAccount = destinationAccount % length;
            }
        }
        if (type.equals("seer")) {
            while (running) {

                if (transactionsSeer.size() > 0) {
                    boolean found = false;
//                    transactionLock.writeLock().lock();
                    transactionLock.writeLock().lock();
                    List<Transaction> batch = transactionsSeer.remove(get);

                    transactionLock.writeLock().unlock();
//                    transactionLock.writeLock().unlock();
                    if (batch == null) {
                        System.out.println("Abort");
                        continue;
                    }
                    for (Transaction item : batch) {
                        for (int i = 0; i < threadCount; i++) {

                            if (threads.get(i).accounts[item.sourceAccount].balance >= item.amount) {

                                threads.get(i).submit(item);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found) {
                        System.out.println("Not enough money in any account");
                    }
                }
                // System.out.println("Seer");
            }
        }
    }

    private void queue(Transaction item) {
        List<Transaction> trans = new ArrayList<>();
        trans.add(item);
        transactionLock.writeLock().lock();
        this.transactionsSeer.add(trans);
        transactionLock.writeLock().unlock();

    }

    private void submit(Transaction transaction) {
        ArrayList newTransactions = new ArrayList<>();
        newTransactions.add(transaction);
        transactionLock.writeLock().lock();
        transactions.add(newTransactions);
        transactionLock.writeLock().unlock();
    }

    public static void main(String[] args) throws InterruptedException {
        Random rng = new Random();
        int threadCount = 12;
        int accountsSize = 120000;

        long size = Long.MAX_VALUE / threadCount;


        List<ShardedBankNonRandom> threads = new ArrayList<>(threadCount + 1);
        Account[] accountShard = new Account[accountsSize];
        for (int j = 0; j < accountsSize; j++) {
            accountShard[j] = new Account(0, threadCount);
        }
        ShardedBankNonRandom seer = new ShardedBankNonRandom(threads, 0, "seer", size,
                0,
                accountShard, null, threadCount);
        threads.add(seer);

        for (int i = 0; i < threadCount; i++) {
            Account[] accountShard2 = new Account[accountsSize];

            for (int j = 0; j < accountsSize; j++) {
                int balance = 75 + rng.nextInt(5000);
                accountShard2[j] = new Account(balance, 0);
            }

            ShardedBankNonRandom shardedTotalOrder = new ShardedBankNonRandom(threads, i, "transacter", size,
                    0,
                    accountShard2, seer, threadCount);
            threads.add(shardedTotalOrder);
        }
        int total = 0;
        for (int i = 0; i < accountsSize; i++) {

            for (int x = 0; x < threadCount; x++) {
                total += threads.get(x).accounts[i].balance;
            }

        }
        System.out.println("Created test data");
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).stopping = true;
            threads.get(i).running = false;
        }
        long end = System.currentTimeMillis();

        Thread.sleep(1000);
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).running = false;
            threads.get(i).stopping = false;
        }
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
        }

        System.out.println("Verifying account balances");
        int total2 = 0;
        for (int i = 0; i < accountsSize; i++) {

            for (int x = 0; x < threadCount; x++) {
                total2 += threads.get(x).accounts[i].balance;
            }

        }
        assert total == total2;
        System.out.println("Finished");

        long totalRequests = 0;

        for (int i = 0; i < threads.size(); i++) {
            System.out.println(String.format("Adding %d %d", i, threads.get(i).transactionsApplied));
            totalRequests += threads.get(i).transactionsApplied;
        }
        long transactionsGenerated = 0;
        for (int i = 0; i < threads.size(); i++) {
            System.out.println(String.format("Generated %d %d", i, threads.get(i).transactionsGenerated));
            transactionsGenerated += threads.get(i).transactionsGenerated;
        }

        double seconds = (end - start) / 1000.0;


//        int n = 0;
//        int thread = 0;
//        int size1 = threads.size();
//        boolean dataAvailable = true;
//        while (dataAvailable) {
//            n = thread + 1;
//            if (n == size1) {
//                thread = 0;
//                n++;
//            }
//            System.out.println(threads.get(thread).data2.get(n));
//            dataAvailable = n < sizes[thread];
//        }
//        int n = 0;
//        int thread = 0;
//        int size1 = threads.size();
//        boolean dataAvailable = true;
//        while (thread < size1) {
//
//            System.out.println(threads.get(thread).data2.get(n));
//            n = n + 1;
//            if (n == sizes[thread]) {
//                thread++;
//                n = 0;
//            }
//        }
        /* code to iterate through all items in order
         * threads refers to one of the lists */
//        int sizes[] = new int[threads.size()];
//        for (int i = 0 ; i < threads.size(); i++) {
//            sizes[i] = threads.get(i).data2.size();
//        }
//        int n = 0;
//        int thread = 0;
//        int size1 = threads.size();
//        int offset = 0;
//        long iterationStart = System.nanoTime();
//        while (thread < size1) {
//
//            // System.out.println(String.format("%d %d", thread, offset + threads.get(thread).data2.get(n)));
//            int current = offset + threads.get(thread).data2.get(n);
//            n = n + 1;
//            if (n == sizes[thread]) {
//                offset += sizes[thread];
//                thread++;
//                n = 0;
//            }
//        }
//        long iterationEnd = System.nanoTime();
//        long iterationTime = iterationEnd - iterationStart;
//        long numberOfItems = 0;
//        for (Integer amount : sizes) {
//            numberOfItems += amount;
//        }
//        System.out.println(String.format("Iteration time %d ns", iterationTime));
//        System.out.println(String.format("Number of items %d", numberOfItems));
//        System.out.println(String.format("Iteration cost %d", iterationTime / numberOfItems ));
//        System.out.println("Lookup");
//        long lookupStart = System.nanoTime();
//
//        int lookupKey = 329131;
//        int current = lookupKey;
//        int currentThread = 0;
//        int total = 0;
//        while (current >= 0 && currentThread <= size1 - 1) {
//            int next = current - sizes[currentThread];
//
//            if (next >= 0) {
//                total += sizes[currentThread];
//                current -= sizes[currentThread];
//                currentThread++;
//
//            } else {
//                break;
//            }
//
//        }
//        long lookupEnd = System.nanoTime();
//        long lookupTime = lookupEnd - lookupStart;
//        System.out.println(String.format("%d %d",
//                currentThread,
//                total + threads.get(currentThread).data2.get(current)));
        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
//        System.out.println(String.format("Lookup time %dns", lookupTime));
        assert transactionsGenerated == totalRequests : String.format("%d != %d", totalRequests, transactionsGenerated);

    }

    private static class Account {
        private final ReentrantReadWriteLock lock;
        private Integer balance;

        public Account(Integer balance, int threadsCount) {
            this.balance = balance;
            this.lock = new ReentrantReadWriteLock();
        }

        public void lock() {
            this.lock.writeLock().lock();
        }

        public void unlock() {
            this.lock.writeLock().unlock();
        }
    }

    private class Transaction {
        private final int sourceAccount;
        private final int destinationAccount;
        private final int amount;
        public boolean done;

        public Transaction(int sourceAccount, int destinationAccount, int amount) {
            this.sourceAccount = sourceAccount;
            this.destinationAccount = destinationAccount;
            this.amount = amount;
        }
    }
}
