package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Arrays.asList;
import static main.BankAccounts2.OPERATION.BALANCE_GET;

public class BankAccounts2 extends Thread {
    private final int id;
    private int totalMoney;
    public boolean synchronizer = false;
    List<Account> accounts;
    private boolean running = true;
    private List<Event> pending;
    private List<Callback> callbacks;
    private List<Event> events;
    private Lock[] writeLocks;
    private Lock[] readLocks;
    private Lock myLock;
    private List<BankAccounts2> threads;
    private int threadsSize;
    private BankAccounts2 synchronizerThread;
    private int requests;
    private int totalSynchronizations;
    private int size;
    private List<Request> pendingRequests;
    private int lastLock = 0;
    private Lock allLock;
    private ReentrantReadWriteLock allLockRw;
    private volatile int counter = 0;

    public BankAccounts2(int size, int id, boolean synchronizer, int totalMoney, List<Account> accounts) {
        this.size = size;
        this.synchronizer = synchronizer;
        this.id = id;
        this.totalMoney = totalMoney;
        this.accounts = accounts;
        this.pending = new ArrayList<>();
        this.callbacks = new ArrayList<>();
        this.pendingRequests = new ArrayList<>();
    }

    public void setThreads(BankAccounts2 synchronizerThread, List<BankAccounts2> threads) {
        this.threads = threads;
        this.threadsSize = threads.size();
        this.readLocks = new Lock[this.threadsSize];
        this.writeLocks = new Lock[this.threadsSize];
        if (this.synchronizer) {


            for (int i = 0; i < this.threadsSize; i++) {
                ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
                this.writeLocks[i] = reentrantReadWriteLock.writeLock();
                this.readLocks[i] = reentrantReadWriteLock.readLock();
            }
            this.allLockRw = new ReentrantReadWriteLock();
            allLock = allLockRw.writeLock();
        } else {
            myLock = synchronizerThread.writeLocks[id];
            allLockRw = synchronizerThread.allLockRw;
        }

    }

    public void run() {
        Random rng = new Random();
        while (running) {
            if (synchronizer) {

                boolean allStopped = true;

                for (int i = lastLock; i < writeLocks.length; i++) {
                    /* boolean locked = */
                    this.writeLocks[i].lock();
//                    if (!locked) {
//                        lastLock = i;
//                        // System.out.println(String.format("%d %d", lastLock, writeLocks.length));
////                        for (int j = i - 1; j >= 0; j--) {
////                            this.writeLocks[j].unlock();
////                        }
//                        allStopped = false;
//                        break;
//
//                    }

                }
                // System.out.println(String.format("Locked up to %d", lastLock));
                if (allStopped) {
                    for (int i = 0; i < threads.size(); i++) {
                        for (Request request : threads.get(i).pendingRequests) {
                            for (Event event : request.pending) {
                                event.execute();
                            }
                            for (Callback callback : request.callbacks) {
                                List<Event> transact = callback.transact(request.pending);
                                for (Event event : transact) {
                                    event.execute();
                                }
                            }
                            request.pending.clear();
                            request.callbacks.clear();
                        }
                        threads.get(i).requests += threads.get(i).pendingRequests.size();
                        threads.get(i).pendingRequests.clear();

                    }


//                    for (int i = 1; i < this.threadsSize -1; i++) {
//                        if (this.threads.get(i).needsAdjustment) {
//                            this.threads.get(i).needsAdjustment = false;
//                            this.threads.get(i).max = this.threads.get(i).next_max;
//                            this.threads.get(i).limit = this.threads.get(i).next_limit;
//                        }
//                    }

//                    long newStart = last_max;

//                    for (Integer thread : toSchedule) {
//
//                        this.threads.get(thread).max = newStart;
//                        this.threads.get(thread).limit = newStart + size;
//                        newStart += size;
//                    }
//                   for (int i = 0 ; i < this.threadsSize; i++) {
//                       threads.get(i).committed = 0;
//                   }
//                    int sum = 0;
//                    for (Account account : accounts) {
//                        sum += account.balance;
//                    }
//
//                    assert totalMoney == sum : String.format("%d != %d", sum, totalMoney);
                    totalSynchronizations++;
                    lastLock = 0;
                    for (int i = 0 ; i < this.threadsSize; i++) {
                        threads.get(i).counter = 2;
                    }
                    for (int i = 0; i < this.writeLocks.length; i++) {
                        this.writeLocks[i].unlock();
                    }


                    // allLock.unlock();
                }
                // Thread.yield();
            } else {
                if (counter > 0) {
//                    if (allLockRw.isWriteLocked()) {
//                        continue;
//                    }
                    List<Request> createdRequests = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        List<Event> newPending = new ArrayList<>();
                        List<Callback> newCallbacks = new ArrayList<>();
                        Request request = new Request(newPending, newCallbacks);
                        createdRequests.add(request);
                        int sourceAccount = 0;
                        int destinationAccount = 0;
                        while (sourceAccount == destinationAccount) {
                            sourceAccount = rng.nextInt(accounts.size());
                            destinationAccount = rng.nextInt(accounts.size());
                            if (sourceAccount == destinationAccount) {
                                destinationAccount = (sourceAccount + 1) % accounts.size();
                            }
                        }


                        int amount = rng.nextInt(1000);
                        newPending.add(new Event(BALANCE_GET, accounts.get(sourceAccount), -1, amount));
                        newPending.add(new Event(BALANCE_GET, accounts.get(destinationAccount), -1, amount));
                        newCallbacks.add(new Callback() {
                            @Override
                            public List<Event> transact(List<Event> events) {

                                int amountDeducted = events.get(0).value2;
                                int accountBalance1 = events.get(0).account.balance;
                                int accountBalance2 = events.get(1).account.balance;
                                assert accountBalance1 == events.get(0).value;
                                // System.out.println(String.format("Deduct %d from %d", amountDeducted, accountBalance1));
                                Event deduction = new Event(OPERATION.BALANCE_SET,
                                        events.get(0).account,
                                        accountBalance1 - amountDeducted, -1);
                                Event addition = new Event(OPERATION.BALANCE_SET,
                                        events.get(1).account,
                                        accountBalance2 + amountDeducted, -1);
                                List<Event> events1 = asList(deduction, addition);
                                // System.out.println(events1);
                                return events1;
                            }
                        });


                    }
                    myLock.lock();
                    pendingRequests.addAll(createdRequests);

                    myLock.unlock();
                    counter--;
                }

            }
        }
    }


    public static void main(String[] args) throws InterruptedException {
        int accountCount = 5;
        int threadCount = 11;
        int size = 2500;
        int id = 0;
        int totalMoney = 0;

        List<BankAccounts2> threads = new ArrayList<>();

        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < accountCount; i++) {
            accounts.add(new Account(i));
        }
        System.out.println("Start state");
        for (Account account : accounts) {
            System.out.println(account.balance);
        }
        for (Account account : accounts) {
            totalMoney += account.balance;
        }
        System.out.println(String.format("= Total Money = %d", totalMoney));

        BankAccounts2 synchronizer = new BankAccounts2(size, id++, true, totalMoney, accounts);
        threads.add(synchronizer);
        for (int i = 0; i < threadCount; i++) {
            BankAccounts2 bankAccounts2 = new BankAccounts2(size, id++, false, totalMoney, accounts);
            threads.add(bankAccounts2);
        }
        synchronizer.setThreads(null, new ArrayList<>(threads));
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).setThreads(synchronizer, new ArrayList<>(threads));
        }

        for (int i = 0; i < threadCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();
        Thread.sleep(5000);
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).running = false;
        }
        synchronizer.running = false;
        synchronizer.join();
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).join();
        }
        long end = System.currentTimeMillis();
        System.out.println("Final state");
        for (Account account : accounts) {
            System.out.println(account.balance);
        }
        totalMoney = 0;
        for (Account account : accounts) {
            totalMoney += account.balance;
        }
        System.out.println(String.format("= Total Money = %d", totalMoney));
        int totalRequests = 0;
        for (BankAccounts2 thread : threads) {
            totalRequests += thread.requests;
        }
        double seconds = (end - start) / 1000.0;
        System.out.println(String.format("%d total requests", totalRequests));
        System.out.println(String.format("%d total synchronizations", synchronizer.totalSynchronizations));
        double l = totalRequests / seconds;
        double s = synchronizer.totalSynchronizations / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("%f synchronizations per second", s));

    }


    private static class Account {
        private volatile int balance;
        private int id;

        Account(int id) {
            this.id = id;
            this.balance = new Random().nextInt(1000);
        }

        public String toString() {
            return String.valueOf(id);
        }
    }

    public enum OPERATION {
        BALANCE_GET,
        BALANCE_SET
    }

    private class Event {
        public int value2;
        Account account;
        int value;
        private OPERATION operation;

        public Event(OPERATION operation, Account account, int value, int value2) {
            this.operation = operation;
            this.account = account;
            this.value = value;
            this.value2 = value2;
        }

        public String toString() {
            switch (operation) {
                case BALANCE_GET:
                    return String.format("Get balance of account %s", account);

                case BALANCE_SET:
                    return String.format("Set balance of account %s to %d", account, value);

            }
            return "OP";
        }

        public void execute() {
            switch (this.operation) {
                case BALANCE_GET:
                    this.value = account.balance;
                    break;
                case BALANCE_SET:
                    account.balance = value;
                    break;
            }
        }
    }

    private interface Callback {
        public List<Event> transact(List<Event> events);
    }


    private class Request {
        private final List<Event> pending;
        private final List<Callback> callbacks;

        public Request(List<Event> pending, List<Callback> callbacks) {

            this.pending = pending;
            this.callbacks = callbacks;
        }
    }
}
