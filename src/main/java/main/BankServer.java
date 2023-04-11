package main;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BankServer extends Thread {

    private static final int DEFAULT_TIME_PORT = 23;
    private static int nextThread = 0;
    private List<BankServer> threads;
    private int threadCount;
    private String type;
    private int id;
    private boolean serve;
    private volatile boolean running = true;
    private long[] accounts;
    private boolean stopping;
    private List<List<Transaction>> transactions;
    private ReadWriteLock transactionLock;
    private int transactionsApplied;
    private int transactionsGenerated;
    private BankServer seer;

    private List<List<Transaction>> transactionsSeer;
    private int transactionsQueued;

    public BankServer(int id, int threadCount, boolean serve, String type, long[] accounts, BankServer seerThread) {
        this.id = id;
        this.serve = serve;
        this.type = type;
        this.transactionLock = new ReentrantReadWriteLock();
        this.threadCount = threadCount;
        this.transactionsSeer = new ArrayList<>();
        this.transactions = new ArrayList<>();
        this.accounts = accounts;
        this.seer = seerThread;
    }

    // Accept connections for current time. Lazy Exception thrown.
    private void acceptConnections(int port) throws Exception {
        // Selector for incoming time requests
        Selector acceptSelector = SelectorProvider.provider().openSelector();

        // Create a new server socket and set to non blocking mode
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);

        // Bind the server socket to the local host and port

        InetAddress lh = InetAddress.getLocalHost();
        InetSocketAddress isa = new InetSocketAddress(lh, port);
        ssc.socket().bind(isa);

        // Register accepts on the server socket with the selector. This
        // step tells the selector that the socket wants to be put on the
        // ready list when accept operations occur, so allowing multiplexed
        // non-blocking I/O to take place.
        SelectionKey acceptKey = ssc.register(acceptSelector,
                SelectionKey.OP_ACCEPT);

        int keysAdded = 0;

        // Here's where everything happens. The select method will
        // return when any operations registered above have occurred, the
        // thread has been interrupted, etc.
        System.out.println(String.format("listening on port %d", port));
        while ((keysAdded = acceptSelector.select()) > 0 && running) {
            // Someone is ready for I/O, get the ready keys
            Set<SelectionKey> readyKeys = acceptSelector.selectedKeys();
            Iterator<SelectionKey> i = readyKeys.iterator();

            // Walk through the ready keys collection and process date requests.
            while (i.hasNext() && running) {
                SelectionKey sk = (SelectionKey) i.next();
                i.remove();
                // The key indexes into the selector so you
                // can retrieve the socket that's ready for I/O
                ServerSocketChannel nextReady = (ServerSocketChannel) sk
                        .channel();
                // Accept the date request and send back the date string
                Socket s = nextReady.accept().socket();
                s.setTcpNoDelay(true);
                // Write the current time to the socket
                InputStream reader = s.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(reader));
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                System.out.println("Trying to read");

                List<Transaction> buffer = new ArrayList<>();
                while (running) {
//                    System.out.println("Received transaction");
                    try {
                        int sourceAccount = dataInputStream.readInt();
                        int destinationAccount = dataInputStream.readInt();
                        long amount = dataInputStream.readLong();
                        Transaction item = new Transaction(sourceAccount, destinationAccount, amount);
//                    System.out.println(item);
                        buffer.add(item);
                        if (buffer.size() >= 100) {
                            threads.get(pickThread()).submit(buffer);
                            transactionsQueued += buffer.size();
                            buffer = new ArrayList<>();
                        }



                        out.println(1);
                    } catch (IOException io) {
                        running = false;
                        System.out.println("Reached end");
                    }
                }
                transactionsQueued += buffer.size();
                threads.get(pickThread()).submit(buffer);

                System.out.println("Finished");

                out.close();
                if (!running) {
                    break;
                }
            }
            if (!running) {
                break;
            }
        }

    }

    public void run() {
        if (serve) {
            try {
                acceptConnections(DEFAULT_TIME_PORT + id);
            } catch (Exception e) {
                System.out.println(e);
            }
        } else {
            while (running) {
                int length = accounts.length;
                int sourceAccount = 0;
                int destinationAccount = 1;
                if (type.equals("executor")) {
                    while (running || stopping || transactions.size() > 0) {
                        if (transactions.size() >= 1) {
                            this.transactionLock.writeLock().lock();
                            List<Transaction> remove = transactions.remove(0);
                            this.transactionLock.writeLock().unlock();
                            boolean removed = false;
                            if (remove != null) {
//                                System.out.println("Received transactions");
                                for (Transaction transaction : remove) {

                                    if (accounts[transaction.sourceAccount] < transaction.amount) {
//                                        System.out.println("Not enough money in account");
                                        seer.queue(List.of(transaction));
                                    } else {

                                        transactionsApplied++;
                                        accounts[transaction.sourceAccount] -= transaction.amount;
                                        accounts[transaction.destinationAccount] += transaction.amount;
                                    }
                                }
                            } else {
                                System.out.println("Failed message");
                            }
                        }
                        if (stopping) {
                            continue;
                        }


                    }
                }
                if (type.equals("seer")) {
                    while (running || transactionsSeer.size() > 0) {

                        if (transactionsSeer.size() > 0) {
                            boolean found = false;
//                    transactionLock.writeLock().lock();
                            transactionLock.writeLock().lock();
                            List<Transaction> batch = transactionsSeer.remove(0);

                            transactionLock.writeLock().unlock();
//                    transactionLock.writeLock().unlock();
                            if (batch == null) {
                                System.out.println("Abort");
                                continue;
                            }
                            for (Transaction item : batch) {
                                for (int i = 0; i < threadCount; i++) {
                                    if (threads.get(i).accounts[item.sourceAccount] >= item.amount) {
                                        threads.get(i).submit(List.of(item));
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
        }
    }


    private void queue(List<Transaction> transactions) {
        transactionLock.writeLock().lock();
        this.transactionsSeer.add(transactions);
        transactionLock.writeLock().unlock();
    }
    private void submit(List<Transaction> transactions) {
        transactionLock.writeLock().lock();
        this.transactions.add(transactions);
        transactionLock.writeLock().unlock();
    }


    private int pickThread() {
        nextThread = (nextThread + 1) % threads.size();
        return nextThread;
    }

    // Entry point.
    public static void main(String[] args) throws InterruptedException {
        // Parse command line arguments and
        // create a new timeserver (no arguments yet)
        int threadCount = 4;
        int accountsSize = 120000;
        List<BankServer> threads = new ArrayList<>();
        BankServer seerThread = new BankServer(-1, threadCount, false, "seer", new long[120000], null);
        for (int i = 0 ; i < threadCount; i++) {
            long[] accountShard2 = new long[accountsSize];

            for (int j = 0; j < accountsSize; j++) {
                int balance = 75 * 4;
                accountShard2[j] = balance;
            }
            threads.add(new BankServer(i, threadCount, false, "executor", accountShard2, seerThread));
        }
        seerThread.setThreads(new ArrayList<>(threads));
        seerThread.start();
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));

        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).start();
        }
        List<BankServer> serverThreads = new ArrayList<>();
        int serverCount = 12;
        for (int i = 0 ; i < serverCount; i++) {
            BankServer nbt = new BankServer(i, threadCount, true, "server", new long[1], seerThread);
            serverThreads.add(nbt);
            try {
                nbt.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            nbt.setThreads(threads);
        }
        int interval = 60;
        Thread.sleep(interval * 1000);
        for (int i = 0 ; i < serverCount; i++) {
            serverThreads.get(i).running = false;
        }
        Thread.sleep(1000);
        seerThread.running = false;
        System.out.println("joining server thread");
        for (int i = 0 ; i < serverCount; i++) {

            serverThreads.get(i).join();
        }

        seerThread.join();
        System.out.println("Server thread joined");

        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).running = false;
        }

        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).join();
            System.out.println(String.format("Thread joined", i));
        }
        long transactions = 0;
        for (int x = 0; x < threadCount; x++) {
            transactions += threads.get(x).transactionsApplied;
        }
        long queued = 0;
        for (int i = 0 ; i < serverCount; i++) {
            queued += serverThreads.get(i).transactionsQueued;

        }
        System.out.println(String.format("Number of transactions applied: %d", transactions));
        System.out.println(String.format("Number of transactions queued: %d", queued));
        assert seerThread.transactionsSeer.size() == 0;
        for (int i = 0 ; i < threadCount; i++) {
            assert threads.get(i).transactions.size() == 0 : threads.get(i).transactions.size();
        }
        System.out.println(String.format("Requests per second %d", transactions / interval));
    }

    private void setThreads(List<BankServer> threads) {
        this.threads = threads;
    }

    private static class Transaction {
        private final int sourceAccount;
        private final int destinationAccount;
        private final long amount;

        public Transaction(int sourceAccount, int destinationAccount, long amount) {
            this.sourceAccount = sourceAccount;
            this.destinationAccount = destinationAccount;
            this.amount = amount;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("source account\n");
            sb.append(sourceAccount);
            sb.append("destination account\n");
            sb.append(destinationAccount);
            sb.append("amount\n");
            sb.append(amount);
            return sb.toString();
        }
    }
}