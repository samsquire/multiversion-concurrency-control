package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShardedNonRandomBank2 extends Thread {
    private final Account[] accounts;
    private int threadId;
    private final String type;
    private volatile boolean running = true;
    private int threadCount;
    private List<ShardedNonRandomBank2> threads;
    private int transactionCount = 0;

    public ShardedNonRandomBank2(List<ShardedNonRandomBank2> threads,
                                 int threadId,
                                 String type,
                                 Account[] accounts,
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
        int destinationAccount = 0;
        int size = accounts.length;

            while (running) {
                ++sourceAccount;
                destinationAccount++;
                sourceAccount = sourceAccount % accounts.length;
                destinationAccount = destinationAccount % accounts.length;
//                System.out.println(String.format("%d -> %d", sourceAccount, destinationAccount));
                int amount = 0;
                Account account = accounts[sourceAccount];
                if (account.balance >= 1) {
                    amount = 75; /* rng.nextInt(account.balance); */
                } else {
                    continue;
                }



                    transactionCount++;
                    account.balance -= amount;
                    accounts[destinationAccount].balance += amount;
            }
    }

    public static void main(String[] args) throws InterruptedException {
        Random rng = new Random();
        int threadsCount = 12;
        System.out.println(String.format("Thread count: %d", threadsCount));
        int accountsSize = 80000;
        System.out.println(String.format("Account count: %d", accountsSize));

        List<ShardedNonRandomBank2> threads = new ArrayList<>(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            Account[] accountShard2 = new Account[accountsSize];
            for (int j = 0; j < accountsSize; j++) {
                accountShard2[j] = new Account(1 + rng.nextInt(100000), 0);
            }
            ShardedNonRandomBank2 shardedTotalOrder = new ShardedNonRandomBank2(threads, i, "transacter",
                    accountShard2, threadsCount);
            threads.add(shardedTotalOrder);
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
    }

    private static class Account {
        private int balance;

        public Account(int balance, int threadsCount) {
            this.balance = balance;
        }

    }


}
