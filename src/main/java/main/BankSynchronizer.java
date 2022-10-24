package main;

import java.util.*;

import static java.lang.Math.floor;

public class BankSynchronizer extends Thread {
    private List<Transaction> waitingDispatch;
    private static final int NEITHER = -1;
    private final int id;
    private final List<BankSynchronizer.Account> accounts;
    private int accountBucket;
    private int[] writing;
    public Map<Integer, List<Transaction>> queued = new HashMap<>();
    private boolean running = true;
    private int requests;
    private ArrayList<BankSynchronizer> threads;
    private int bucket;
    private boolean synchronizer;
    private ArrayList<BankSynchronizer> synchronizerThreads;
    private int bucketSize;
    private int synchronizerSize;
    private int threadsSize;

    public BankSynchronizer(int bucket, int bucketSize, boolean synchronizer, int id, List<BankSynchronizer.Account> accounts) {
        this.bucket = bucket;
        this.synchronizer = synchronizer;
        this.id = id;
        this.accounts = accounts;
        this.bucketSize = bucketSize;
        this.waitingDispatch = new ArrayList<>();
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
        int threadCount = 100;
        int bucketSize = 10;
        int synchronizerThreads = 5;
        List<BankSynchronizer.Account> accounts = new ArrayList<>();
        int id = 0;
        int money = 0;
        for (int i = 0; i < accountCount; i++) {
            accounts.add(new BankSynchronizer.Account(id++));
        }
        for (BankSynchronizer.Account account : accounts) {
            money += account.balance;
        }
        System.out.println(String.format("= Total money = %d", money));
        int threadId = 0;


        List<BankSynchronizer> threads = new ArrayList<>();
        List<BankSynchronizer> allSynchronizerThreads = new ArrayList<>();
        int bucket = 0;
        for (int i = 0; i < synchronizerThreads; i++) {
            BankSynchronizer synchronizerThread = new BankSynchronizer(bucket, bucketSize, true, threadId++, accounts);
            threads.add(synchronizerThread);
            allSynchronizerThreads.add(synchronizerThread);

        }
        for (int i = 0; i < threadCount; i++) {
            threads.add(new BankSynchronizer(bucket, bucketSize,false, threadId++, accounts));
            bucket += 5;
        }
        for (int i = 0; i < synchronizerThreads; i++) {
            allSynchronizerThreads.get(i).setThreads(new ArrayList<BankSynchronizer>(allSynchronizerThreads), new ArrayList<>(threads));
        }
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).setThreads(new ArrayList<BankSynchronizer>(allSynchronizerThreads), new ArrayList<>(threads));
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
        for (BankSynchronizer.Account account : accounts) {
            totalMoney += account.balance;
        }

        assert totalMoney == money : totalMoney;
        System.out.println(String.format("= Total Money = %d", totalMoney));
        int totalRequests = 0;
        for (BankSynchronizer thread : threads) {
            totalRequests += thread.requests;
        }
        double seconds = (end - start) / 1000.0;
        System.out.println("Finished");
        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
    }

    private void setThreads(ArrayList<BankSynchronizer> synchronizerThreads, ArrayList<BankSynchronizer> threads) {
        this.threads = threads;
        this.synchronizerThreads = synchronizerThreads;
        this.synchronizerSize = synchronizerThreads.size();
        this.threadsSize = threads.size();
        this.writing = new int[threadsSize];
        Arrays.fill(this.writing, NEITHER);
        for (Account account : accounts) {
            queued.put(account.id, new ArrayList<>());
        }
    }

    public void run() {
        Random rng = new Random();
        while (running || waitingDispatch.size() > 0) {
            if (synchronizer) {
                boolean subcheck = false;
                boolean fail = false;

                int targetMode = 0;

                for (int j = 0; j < id; j++) {
                    if (writing[j] == targetMode) {
                        fail = true;

                        break;
                    } // data.reading test
                }
                for (int j = id + 1; j < threadsSize; j++) {
                    if (writing[j] == targetMode) {
                        fail = true;
                        break;
                    } // data.reading test
                }
                // data.reading loop


                if (!fail) {
                    writing[id] = targetMode;

                    for (int j = threadsSize - 1; j >= 0; j--) {
                        if (j != id && writing[j] == targetMode) {


                            subcheck = true;
                            break;
                        } // data.reading check
                    } // data.reading loop

                    if (!subcheck) {
                        for (int j = 0; j < threadsSize; j++) {
                            if (j != id && writing[j] == targetMode) {


                                subcheck = true;
                                break;
                            } // data.reading check
                        } // data.reading loop

                    }
                    if (!subcheck) {
                        assert writing[id] == targetMode;
                        for (Map.Entry<Integer, List<BankSynchronizer.Transaction>> entry : queued.entrySet()) {
                            for (Transaction transaction : entry.getValue()) {
                                // accounts.get(transaction.account).balance += transaction.amount;
                            }
                        }
                        for (Integer accountNo : queued.keySet()) {
                            queued.get(accountNo).clear();
                        }
                        writing[id] = NEITHER;
                        // System.out.println("Successful exclusive execute");


                        // Thread.yield();
                    } else {
                        writing[id] = NEITHER;
                    }

                } // subcheck doubly safe
                else {
                    writing[id] = NEITHER;
                }
            }
            if (!synchronizer) {
                for (Transaction transaction : waitingDispatch) {
                    this.dispatch(transaction);
                }
                waitingDispatch.clear();
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
                Transaction out = new Transaction(sourceAccount, -amount);
                if (!this.dispatch(out)) {
                    waitingDispatch.add(out);
                }
                Transaction in = new Transaction(destinationAccount, amount);
                if (!this.dispatch(in)) {
                    waitingDispatch.add(in);
                }
                    requests++;
            }
        }
    }

    private boolean dispatch(Transaction transaction) {
        int account = transaction.account;
        int synchronizerThread = (int) floor(account / bucketSize);
        boolean subcheck = false;
        boolean fail = false;

        int targetMode = 0;

        for (int j = 0; j < id; j++) {
            if (synchronizerThreads.get(synchronizerThread).writing[j] == targetMode) {
                fail = true;

                break;
            } // data.reading test
        }
        for (int j = id + 1; j < threadsSize; j++) {
            if (synchronizerThreads.get(synchronizerThread).writing[j] == targetMode) {
                fail = true;
                break;
            } // data.reading test
        }
        // data.reading loop


        if (!fail) {
            synchronizerThreads.get(synchronizerThread).writing[id] = targetMode;

            for (int j = threadsSize - 1; j >= 0; j--) {
                if (j != id && synchronizerThreads.get(synchronizerThread).writing[j] == targetMode) {


                    subcheck = true;
                    break;
                } // data.reading check
            } // data.reading loop

            if (!subcheck) {
                for (int j = 0; j < threadsSize; j++) {
                    if (j != id && synchronizerThreads.get(synchronizerThread).writing[j] == targetMode) {


                        subcheck = true;
                        break;
                    } // data.reading check
                } // data.reading loop

            }
            if (!subcheck) {
                assert synchronizerThreads.get(synchronizerThread).writing[id] == targetMode;
                synchronizerThreads.get(synchronizerThread).queued.get(account).add(transaction);
                synchronizerThreads.get(synchronizerThread).writing[id] = NEITHER;
                // System.out.println("Successful exclusive execute");


                // Thread.yield();
            } else {

                synchronizerThreads.get(synchronizerThread).writing[id] = NEITHER;
                return false;
            }

        } // subcheck doubly safe
        else {

            synchronizerThreads.get(synchronizerThread).writing[id] = NEITHER;
            return false;
        }
        return true;
    }

    private class Transaction {
        private final int account;
        private final int amount;

        public Transaction(int sourceAccount, int amount) {

            this.account = sourceAccount;
            this.amount = amount;
        }
    }
}
