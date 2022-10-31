package main;

import org.apache.groovy.json.internal.CharBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BankAccounts3 extends Thread {
    private final int id;
    private final List<Account> accounts;
    public List<Account> acocunts;
    public int[] owner;
    private boolean running = true;
    private int requests;

    public BankAccounts3(int id, List<Account> accounts) {
        this.id = id;
        this.accounts = accounts;
    }

    private static class Account {
        private int balance;
        private final ReentrantReadWriteLock rwlock;

        public Account(int id) {
            this.balance = new Random().nextInt(2500);
            this.rwlock = new ReentrantReadWriteLock();
        }
        public void lock() {
            rwlock.writeLock().lock();
        }
        public void unlock() {
            rwlock.writeLock().unlock();
        }
    }
    public static void main(String[] args) throws InterruptedException {
        int accountCount = 5;
        int threadCount = 100;
        List<Account> accounts = new ArrayList<>();
        int id = 0;
        int money = 0;
        for (int i = 0; i < accountCount; i++) {
            accounts.add(new Account(id++));
        }
        for (BankAccounts3.Account account : accounts) {
            money += account.balance;
        }
        System.out.println(String.format("= Total Money = %d", money));
        System.out.println("Starting test");

        int threadId = 0;
        List<BankAccounts3> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            threads.add(new BankAccounts3(threadId++, accounts));
        }


        for (int i = 0; i < threadCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).join();
        }
        long end = System.currentTimeMillis();

        int totalMoney = 0;
        for (BankAccounts3.Account account : accounts) {
            totalMoney += account.balance;
        }
        assert totalMoney == money;
        System.out.println(String.format("= Total Money = %d", totalMoney));
        int totalRequests = 0;
        for (BankAccounts3 thread : threads) {
            totalRequests += thread.requests;
        }
        double seconds = (end - start) / 1000.0;
        System.out.println("Finished");
        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
    }
    public void run() {
        Random rng = new Random();
        while (running) {
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
            if (sourceAccount > destinationAccount) {
                accounts.get(destinationAccount).lock();
                accounts.get(sourceAccount).lock();
            } else {
                accounts.get(sourceAccount).lock();
                accounts.get(destinationAccount).lock();
            }
            accounts.get(sourceAccount).balance -= amount;
            accounts.get(destinationAccount).balance += amount;
            if (sourceAccount > destinationAccount) {
                accounts.get(destinationAccount).unlock();
                accounts.get(sourceAccount).unlock();
            } else {
                accounts.get(sourceAccount).unlock();
                accounts.get(destinationAccount).unlock();
            }
            requests++;
        }
    }
}
