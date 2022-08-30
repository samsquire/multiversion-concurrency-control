package main;

import java.util.*;
import java.util.concurrent.atomic.*;


class Main extends Thread {
    private int mailboxes = 100;
    public AtomicInteger next = new AtomicInteger(0);
    public AtomicInteger localNext = new AtomicInteger(0);
    public AtomicInteger sendNext = new AtomicInteger(0);
    public volatile boolean running = true;
    public int threadNum;
    private HashMap<Integer, ArrayList<Message>> outqueue;
    public List<List<Message>> inqueue;
    private volatile boolean reading[][];
    private HashMap<Integer, Integer> mailsize;
    public int requestCount;
    public int contentions = 0;
    private volatile boolean started;
    private int threadsSize;
    private boolean outgoing = false;
    private int sent;
    private int delivered;

    public void setThreads(List<Main> threads) {
        this.threads = threads;
        this.threadsSize = threads.size();

        for (int i = 0 ; i < threadsSize; i++) {
            this.outqueue.put(i, new ArrayList<>(10000));
        }

    }


    private int messageRate;
    private List<Main> threads;

    public Main(int messageRate, int threadNum, List<Main> threads, int size, boolean synchronizer, int mailboxes) {
        this.messageRate = messageRate;
        this.threads = threads;
        this.running = true;
        this.threadNum = threadNum;
        this.outqueue = new HashMap<Integer, ArrayList<Message>>();
        this.inqueue = new ArrayList<>(mailboxes);
        this.mailboxes = mailboxes;
        this.mailsize = new HashMap<>();
        for (int i = 0 ; i < mailboxes ; i++) {
            this.inqueue.add(new ArrayList<Message>(10000));
            mailsize.put(i, 0);

        }
        this.reading = new boolean[mailboxes][size];


    }

    public static void main(String[] args) throws InterruptedException {
        ArrayList<Main> threads = new ArrayList<>();
        int mailboxes = 3;
        int messageRate = 10000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            Main thread = new Main(messageRate, i, threads, 101, false, mailboxes);
            threads.add(thread);
        }

        Main synchronizer = new Main(messageRate, 100, new ArrayList<>(threads), 101, true, mailboxes);
        threads.add(synchronizer);
        synchronizer.setThreads(new ArrayList<>(threads));



        for (int i = 0; i < 100; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }
        for (int i = 0; i < 100; i++) {
            threads.get(i).start();
        }
        synchronizer.start();

        int benchmarkTime = 5000;
        Thread.sleep(benchmarkTime);
        for (int i = 0; i < 100; i++) {
            threads.get(i).running = false;
        }
        threads.get(100).running = false;
        for (int i = 0; i < 100; i++) {
            threads.get(i).join();
        }
        threads.get(100).join();
        int totalRequests = 0;
        for (int i = 0; i < 101; i++) {
            totalRequests += threads.get(i).requestCount;
        }
        long end = System.currentTimeMillis();
        long seconds = (end - start) / 1000;
        System.out.println(String.format("%d total requests", totalRequests));
        System.out.println(String.format("%d requests per second", totalRequests / seconds));
        System.out.println(String.format("Time taken: %d", seconds));
        int contentions = 0;
        for (int i = 0; i < threads.size(); i++) {
            contentions += threads.get(i).contentions;
        }
        System.out.println(String.format("Contentions: %d", contentions));

    }

    public boolean tryConnectToThread(Main main, int inbox) {
        boolean success = false;
        List<Integer> removals = new ArrayList<>();
        boolean foundMailbox = false;
        for (Map.Entry<Integer, ArrayList<Message>> entry : outqueue.entrySet()) {
            List<Message> messages = entry.getValue();
            int messagesSize = messages.size();
            Main thisThread = threads.get(entry.getKey());
            boolean fail = false;
            for (int mailbox = inbox; mailbox < main.mailboxes; mailbox++) {
                for (int j = 0; j < main.threadNum - 1; j++) {


                    if (thisThread.reading[inbox][j]) {
                        fail = true;
                        break;
                    } // reading if

                }    // reading loop
                if (fail) {
                    break; // stop looking at this mailbox
                }
                if (!fail) {
                    for (int j = threadNum + 1; j < main.threadsSize; j++) {

                        if (thisThread.reading[inbox][j]) {
                            fail = true;
                            break;
                        } // reading if

                    }
                }
                if (fail) {
                    break; // stop looking at this mailbox
                }

                boolean subfail = false;
                if (!fail) {
                    thisThread.reading[inbox][main.threadNum] = true;


                    for (int j = main.threadsSize - 1; j > main.threadNum; j--) {
                        if (thisThread.reading[inbox][j]) {
                            subfail = true;
                            thisThread.reading[inbox][main.threadNum] = false;
                            break;
                        } // reading check if
                    }
                    if (subfail) {
                        break; // stop looking at this mailbox
                    }
                    if (!subfail) {
                        for (int j = main.threadNum - 1; j >= 0; j--) {
                            if (thisThread.reading[inbox][j]) {
                                subfail = true;
                                thisThread.reading[inbox][main.threadNum] = false;
                                break;
                            } // reading check if
                        }
                    }
                    // reading loop
                    if (subfail) {
                        break; // stop looking at this mailbox
                    }
                    if (!subfail) {
                        for (int j = 0; j < main.threadNum; j++) {
                            if (thisThread.reading[inbox][j]) {
                                subfail = true;
                                thisThread.reading[inbox][main.threadNum] = false;
                                break;
                            } // reading check if
                        }
                        if (subfail) {
                            break; // stop looking at this mailbox
                        }
                        for (int j = threadNum + 1; j < main.threadsSize; j++) {
                            if (thisThread.reading[inbox][j]) {
                                subfail = true;
                                thisThread.reading[inbox][main.threadNum] = false;
                                break;
                            } // reading check if
                        }

                        // reading loop
                    }
                    if (subfail) {
                        break; // stop looking at this mailbox
                    }

                    if (!subfail) {

                        // System.out.println("successful send");
                        assert thisThread.reading[inbox][main.threadNum] == true;


                        thisThread.inqueue.get(inbox).addAll(messages);

                        thisThread.mailsize.put(inbox, thisThread.mailsize.get(inbox) + messagesSize);
                        thisThread.reading[inbox][main.threadNum] = false;
                        removals.add(entry.getKey());
                        delivered += messagesSize;
                        success = true;
                        foundMailbox = true;

                    } else {
                        // System.out.println("fail");
                        thisThread.reading[inbox][main.threadNum] = false;
                        main.contentions++;
//                            Thread.yield();
                        for (int m = 0; m < main.threadsSize; m++) {
                            boolean reading = false;
                            int t = (m + next.get()) % threadsSize;
                            for (int j = 0; j < main.threadsSize; j++) {
                                if (main.threads.get(t).reading[inbox][j]) {
                                    reading = true;
                                    break;
                                }
                            }
                            if (!reading) {
                                for (Message message : messages) {
                                    message.to = t;
                                }
                                break;
                            }

                        }
                    }

                } // first check !fail
                else {
                    main.contentions++;
                    thisThread.reading[inbox][main.threadNum] = false;
                    for (int m = 0; m < main.threadsSize; m++) {
                        boolean reading = false;
                        int t = (m + next.get()) % threadsSize;
                        for (int j = 0; j < main.threadsSize; j++) {
                            if (main.threads.get(t).reading[inbox][j]) {
                                reading = true;
                                break;
                            }
                        }
                        if (!reading) {
                            for (Message message : messages) {
                                message.to = t;
                            }
                            break;
                        }

                    }

                    //System.out.println("fail send");
//                        Thread.yield();
                }

                if (foundMailbox) {
                    break; // finished finding mailbox for this set of messages
                }
            } // mailbox loop
        } // outqueue loop
        for (Integer key : removals) {
            main.outqueue.get(key).clear();
        }
        return success;
    }

    public boolean tryReceiveInbox(Main main, Run run, int inbox, int depth) {
        boolean subcheck = false;
        boolean fail = false;
        boolean success = false;

        for (int j = 0; j < main.threadNum; j++) {
            if (main.reading[inbox][j]) {
                fail = true;
                break;
            } // reading test
        }
        for (int j = main.threadNum + 1; j < main.threadsSize; j++) {
            if (main.reading[inbox][j]) {
                fail = true;
                break;
            } // reading test
        }
        // reading loop


        if (!fail) {

            main.reading[inbox][main.threadNum] = true;


            for (int j = main.threadsSize - 1; j >= 0; j--) {
                if (j != main.threadNum && main.reading[inbox][j]) {


                    subcheck = true;
                    break;
                } // reading check
            } // reading loop

            if (!subcheck) {
                for (int j = 0; j < main.threadsSize; j++) {
                    if (j != main.threadNum && main.reading[inbox][j]) {


                        subcheck = true;
                        break;
                    } // reading check
                } // reading loop

            }
            if (!subcheck) {
                assert main.reading[inbox][main.threadNum] == true;
                success = true;


                for (Message message : main.inqueue.get(inbox)) {

                    main.requestCount++;


                    // System.out.println(String.format("%d received %d from %d", threadNum, message.body, message.from));
                }
                main.mailsize.put(inbox, main.mailsize.get(inbox) - main.inqueue.get(inbox).size());

                main.inqueue.get(inbox).clear();
                main.reading[inbox][main.threadNum] = false;
                Thread.yield();

            } // subcheck doubly safe
            else {
                main.reading[inbox][main.threadNum] = false;
//                    Thread.yield();
                main.contentions++;
                if (depth < 1) {
                    for (int m = 0; m < main.threadsSize; m++) {
                        boolean reading = false;
                        for (int j = 0; j < main.threadsSize; j++) {
                            if (main.threads.get(m).reading[inbox][j]) {
                                reading = true;
                                break;
                            }
                        }
                        if (!reading) {
                            if (!main.threads.get(m).started) {

                                main.threads.get(m).reading[inbox][main.threadNum] = true;

                                boolean innerfail = false;
                                for (int j = 0; j < main.threadsSize; j++) {
                                    if (main.threads.get(m).reading[inbox][j]) {
                                        innerfail = true;
                                        break;
                                    }
                                }
                                if (!innerfail) {

                                    transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1);
                                }
                                main.threads.get(m).reading[inbox][main.threadNum] = false;
                                break;
                            }

                        }

                    }
                }

            }
        } // ^ safe to read
        else {


            main.reading[inbox][main.threadNum] = false;
//                Thread.yield();
            // System.out.println("failed to read");
            main.contentions++;
            if (depth < 1) {
                for (int m = 0; m < main.threadsSize; m++) {
                    boolean reading = false;
                    for (int j = 0; j < main.threadsSize; j++) {
                        if (main.threads.get(m).reading[inbox][j]) {
                            reading = true;
                            break;
                        }
                    }
                    if (!reading) {
                        if (!main.threads.get(m).started) {

                            main.threads.get(m).reading[inbox][main.threadNum] = true;
                            boolean innerfail = false;
                            for (int j = 0; j < main.threadsSize; j++) {
                                if (main.threads.get(m).reading[inbox][j]) {
                                    innerfail = true;
                                    break;
                                }
                            }
                            if (!innerfail) {
                                transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1);
                            }
                            main.threads.get(m).reading[inbox][main.threadNum] = false;
                            break;
                        }
                    }


                }
            }


        }
        return success;
    }

    public boolean transaction(Main main, Run run, int depth) {
        main.started = true;
        boolean preempted = false;
        for (int j = 0 ; j < main.mailboxes; j++) {
            preempted = false;
            for (int i = 0; i < main.threadsSize; i++) {
                if (main.reading[j][i]) {
                    preempted = true;
                    break;
                }
            }
            if (preempted) {
                break;
            }

        }

        if (preempted) {
            return false;
        }

        int inboxStart = (main.localNext.getAndAdd(1)) % main.mailboxes;
        boolean success = tryConnectToThread(main, inboxStart);
        // successfully sent a message to a thread


         // message loop

        // try read messages

        boolean successReceive = false;
        for (int inbox = 0 ; inbox < main.mailboxes; inbox++) {
            int t = (main.localNext.getAndAdd(1) + inbox) % main.mailboxes;
            if (main.mailsize.get(inbox) > 0) {
                successReceive = tryReceiveInbox(main, run, t, 0);
            }

        }
        main.started = false;
        return successReceive;
    }

    public void run() {
        Run run = new Run(threadNum, threadNum);
        Random random = new Random();
        List<Main> rthreads = new ArrayList<>(threads);
        Collections.reverse(rthreads);

        while (running || sent != delivered) {
            if (sent == delivered) {
                /**
                 there is contention writing to the same thread, so try spread it out
                 **/

                run.lastThread = (next.getAndAdd(1) + threadNum) % threadsSize;

                for (int i = 0; i < messageRate; i++) {

                    Message message = new Message(threadNum, run.lastThread, run.counter++);

                    this.outqueue.get(message.to).add(message);
                    run.lastThread = (run.lastThread + 1) % threadsSize;

                }
                sent += messageRate;


            } else {
                transaction(this, run, 0);
            }

        }
    }

    public static class Run {
        public int counter;
        public int lastThread;

        public Run(int counter, int lastThread) {
            this.counter = counter;
            this.lastThread = lastThread;
        }
    }

    public static class Message {
        private int from;
        private int to;
        private int body;


        public Message(int threadNum, int destination, int body) {
            this.from = threadNum;
            this.to = destination;
            this.body = body;

        }
    }

}