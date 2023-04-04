package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShardedNonRandomBank2 extends Thread {
    private final long[] accounts;
    private int threadId;
    private final String type;
    private volatile boolean running = true;
    private int threadCount;
    private List<ShardedNonRandomBank2> threads;
    private int transactionCount = 0;

    public ShardedNonRandomBank2(List<ShardedNonRandomBank2> threads,
                                 int threadId,
                                 String type,
                                 long[] accounts,
                                 int threadCount) {
        this.threads = threads;
        this.threadId = threadId;
        this.type = type;
        this.threadCount = threadCount;
        this.accounts = accounts;
    }

    public void run() {
        Random rng = new Random();
        int sourceAccount = 0;
        int destinationAccount = 1;
        int size = accounts.length;

        while (running) {

//                System.out.println(String.format("%d -> %d", sourceAccount, destinationAccount));
            long amount = 0;
            long account = accounts[sourceAccount];
            if (account >= 75) {
                amount = 75; /* rng.nextInt(account.balance); */
            } else {
                continue;
            }


            transactionCount++;
            accounts[sourceAccount] -= amount;
            accounts[destinationAccount] += amount;
            sourceAccount++;
            destinationAccount++;
            sourceAccount = sourceAccount % accounts.length;
            destinationAccount = destinationAccount % accounts.length;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Random rng = new Random();
        int threadsCount = 12;
        System.out.println(String.format("Thread count: %d", threadsCount));
        int accountsSize = 120000;
        System.out.println(String.format("Account count: %d", accountsSize));

        List<ShardedNonRandomBank2> threads = new ArrayList<>(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            long[] accountShard2 = new long[accountsSize];
            for (int j = 0; j < accountsSize; j++) {
                accountShard2[j] = 1 + rng.nextInt(100000);
            }
            ShardedNonRandomBank2 shardedTotalOrder = new ShardedNonRandomBank2(threads, i, "transacter",
                    accountShard2, threadsCount);
            threads.add(shardedTotalOrder);
        }
        long startBalance = 0;
        for (int i = 0; i < threadsCount; i++) {

            for (int x = 0 ; x < threads.get(i).accounts.length; x++) {
                startBalance += threads.get(i).accounts[x];
            }
        }
        System.out.println("Created test data");
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).running = false;
        }

        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
        }

        System.out.println("Finished");
        System.out.println("Verifying");
        long endBalance = 0;
        for (int i = 0; i < threadsCount; i++) {

            for (int x = 0 ; x < threads.get(i).accounts.length; x++) {
                endBalance += threads.get(i).accounts[x];
            }
        }
        long end = System.currentTimeMillis();

        long totalRequests = 0;

        for (int i = 0; i < threads.size(); i++) {
            System.out.println(String.format("Adding %d", threads.get(i).transactionCount));
            totalRequests += threads.get(i).transactionCount;
        }

        double seconds = (end - start) / 1000.0;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
        System.out.println(String.format("Total money beginning: %d", startBalance));
        System.out.println(String.format("Total money end: %d", endBalance));
        assert (startBalance == endBalance);
    }

    private static class Account {
        private int balance;

        public Account(int balance, int threadsCount) {
            this.balance = balance;
        }

    }


}
