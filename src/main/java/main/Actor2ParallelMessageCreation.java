package main;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


class Actor2ParallelMessageCreation extends Thread {
    private int NEITHER = -1;
    private int PREEMPTED = -2;
    private final ArrayList<ArrayList<AlternativeMessage>> messages;
    private int mailboxes;
    public AtomicInteger next = new AtomicInteger(0);
    public AtomicInteger localNext = new AtomicInteger(0);
    public volatile boolean running = true;
    public int threadNum;
    private HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>> outqueue;
    public ArrayList<ArrayList<Slice>> inqueue;
    private volatile int writing[];
    private volatile int reading[][];
    private Integer mailsize;
    public long requestCount;
    public int contentions = 0;
    private volatile boolean started;
    private int threadsSize;
    private int sent;
    private int delivered;
    List<Integer> removals;
    private boolean logging;
    private int batchSize;
    private int subthread;
    private int receiveThreadNum;
    private int numSubthreads;

    public void setThreads(List<Actor2ParallelMessageCreation> threads) {
        this.threads = threads;
        this.threadsSize = threads.size();

        for (int i = 0; i < threadsSize; i++) {
            this.outqueue.put(i, new ArrayList<>(10000));
        }
        this.reading = new int[mailboxes][threadsSize];
        for (int i = 0 ; i < mailboxes; i++) {
            for (int t = 0 ; t < threadsSize; t++) {
                this.reading[i][t] = NEITHER;
            }

        }
        this.writing = new int[threadsSize];
        for (int t = 0 ; t < threadsSize; t++) {
            this.writing[t] = NEITHER;
        }

    }


    private int messageRate;
    private List<Actor2ParallelMessageCreation> threads;
    private boolean creationThread;

    public Actor2ParallelMessageCreation(boolean logging, int batchSize, ArrayList<ArrayList<AlternativeMessage>> messages,
                                         int subthread,
                                         int messageRate,
                                         int threadNum,
                                         int receiveThreadNum,
                                         List<Actor2ParallelMessageCreation> threads,
                                         int size,
                                         boolean synchronizer,
                                         int mailboxes,
                                         int numSubthreads, boolean creationThread) {
        this.logging = logging;
        this.batchSize = batchSize;
        this.subthread = subthread;
        this.numSubthreads = numSubthreads;
        this.messageRate = messageRate;
        this.threads = threads;
        this.creationThread = creationThread;
        this.running = true;
        this.threadNum = threadNum;
        this.receiveThreadNum = receiveThreadNum;
        this.outqueue = new HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>>();
        this.inqueue = new ArrayList<ArrayList<Slice>>(mailboxes);
        for (int i = 0; i <= mailboxes; i++) {
            this.inqueue.add(new ArrayList<>(1000));
        }
        this.mailboxes = mailboxes;
        this.messages = messages;
        this.mailsize = 0;


        this.removals = new ArrayList<>(10000);


    }

    private void subthreadOf(Actor2ParallelMessageCreation actor2) {
        this.inqueue = actor2.inqueue;
        this.outqueue = actor2.outqueue;
    }

    public static void main(String[] args) throws InterruptedException {
        ArrayList<Actor2ParallelMessageCreation> threads = new ArrayList<>();
        ArrayList<Actor2ParallelMessageCreation> allThreads = new ArrayList<>();
        int mailboxes = 4;
        int messageRate = 10000;
        int numSubthreads = 10;
        int threadCount = 100;
        int batchSize = 1;
        boolean logging = false;



        ArrayList<ArrayList<AlternativeMessage>> messages = new ArrayList<>();






        ArrayList<ArrayList<Actor2ParallelMessageCreation>> allSubthreads = new ArrayList<>();
        long start = System.currentTimeMillis();
        int threadNum = 0;
        int totalSize = threadCount * numSubthreads + 1;
        for (int i = 0; i < threadCount; i++) {
            Actor2ParallelMessageCreation thread = new Actor2ParallelMessageCreation(logging, batchSize, messages, 0, messageRate, threadNum++, i, threads, totalSize, false, mailboxes, numSubthreads, false);
            allThreads.add(thread);
            threads.add(thread);
        }
        for (int i = 0; i < numSubthreads; i++) {
            ArrayList<Actor2ParallelMessageCreation> subthreads = new ArrayList<>();
            for (int subthread = 1; subthread < numSubthreads; subthread++) {
                Actor2ParallelMessageCreation thread = new Actor2ParallelMessageCreation(logging, batchSize, messages, subthread, messageRate, threadNum++, i, threads, totalSize, false, mailboxes, numSubthreads, i > 5);
                thread.subthreadOf(allThreads.get(i));
                subthreads.add(thread);

                allThreads.add(thread);
            }
            allSubthreads.add(subthreads);
        }

        Actor2ParallelMessageCreation synchronizer = new Actor2ParallelMessageCreation(logging, batchSize, messages, 0, messageRate, threadNum++, 101, new ArrayList<>(allThreads), totalSize, true, mailboxes, numSubthreads, false);
        allThreads.add(synchronizer);
        threads.add(synchronizer);
        synchronizer.setThreads(new ArrayList<>(allThreads));


        for (int i = 0; i < 100; i++) {
            threads.get(i).setThreads(new ArrayList<>(allThreads));
        }
        for (ArrayList<Actor2ParallelMessageCreation> subthreads : allSubthreads) {
            for (Actor2ParallelMessageCreation subthread : subthreads) {
                subthread.setThreads(new ArrayList<>(allThreads));

            }
        }

        for (int i = 0; i < 100; i++) {
            threads.get(i).start();
        }
        for (ArrayList<Actor2ParallelMessageCreation> subthreads : allSubthreads) {
            for (Actor2ParallelMessageCreation subthread : subthreads) {
                subthread.start();
            }
        }
        synchronizer.start();

        int benchmarkTime = 5000;
        Thread.sleep(benchmarkTime);
        for (int i = 0; i < 100; i++) {
            threads.get(i).running = false;
        }
        for (ArrayList<Actor2ParallelMessageCreation> subthreads : allSubthreads) {
            for (Actor2ParallelMessageCreation subthread : subthreads) {
                subthread.running = false;
            }
        }
        threads.get(100).running = false;
        for (int i = 0; i < 100; i++) {
            threads.get(i).join();
        }

        threads.get(100).join();
        for (ArrayList<Actor2ParallelMessageCreation> subthreads : allSubthreads) {
            for (Actor2ParallelMessageCreation subthread : subthreads) {
                subthread.join();
            }
        }
        long totalRequests = 0;
        for (Actor2ParallelMessageCreation thread : allThreads) {
            totalRequests += thread.requestCount;
        }
        long end = System.currentTimeMillis();
        double seconds = (end - start) / 1000.0;
        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
        int contentions = 0;
        for (int i = 0; i < threads.size(); i++) {
            contentions += threads.get(i).contentions;
        }
        System.out.println(String.format("Contentions: %d", contentions));

    }



    public boolean tryConnectToThread(Actor2ParallelMessageCreation main, int startMailbox, int forThread) {
        boolean success = false;

        boolean subcheck = false;
        boolean fail = false;
        int targetMode = 0;

        for (int j = 0; j < main.threadNum; j++) {
            if (main.writing[j] == targetMode) {
                fail = true;

                break;
            } // reading test
        }
        for (int j = main.threadNum + 1; j < main.threadsSize; j++) {
            if (main.writing[j] == targetMode) {
                fail = true;
                break;
            } // reading test
        }
        // reading loop


        if (!fail) {
            main.writing[main.threadNum] = targetMode;

            for (int j = main.threadsSize - 1; j >= 0; j--) {
                if (j != main.threadNum && main.writing[j] == targetMode) {


                    subcheck = true;
                    break;
                } // reading check
            } // reading loop

            if (!subcheck) {
                for (int j = 0; j < main.threadsSize; j++) {
                    if (j != main.threadNum && main.writing[j] == targetMode) {


                        subcheck = true;
                        break;
                    } // reading check
                } // reading loop

            }
            if (!subcheck && main.outqueue.containsKey(forThread) && main.outqueue.get(forThread).size() > 0) {
                assert main.writing[main.threadNum] == targetMode;
                assert main.outqueue.get(forThread).size() > 0 : main.outqueue.get(forThread).size();
                success = true;

                ArrayList<AlternativeMessage> slice = main.outqueue.get(forThread).remove(0);
                main.writing[main.threadNum] = NEITHER;
                if (slice != null && slice.size() > 0) {
                        boolean foundMailbox = false;
                        boolean subfail = false;
                        fail = false;

                        Actor2ParallelMessageCreation thisThread = threads.get(forThread);

                        for (int mb = 0; mb < main.mailboxes; mb++) {
                            int inbox = (mb + startMailbox) % main.mailboxes;

                            ArrayList<AlternativeMessage> messages = slice;
                            if (messages.size() == 0) {
                                break;
                            }

                            int fallbackMode = -1;
                            targetMode = thisThread.inqueue.get(inbox).size() + 1;
                            int messagesSize = messages.size();
                            for (int j = 0; j < main.threadNum - 1; j++) {


                                if (thisThread.reading[inbox][j] == targetMode) {
                                    fail = true;
                                    break;
                                } // reading if

                            }    // reading loop
                            if (fail) {
                                break; // stop looking at this mailbox
                            }
                            if (!fail) {
                                for (int j = threadNum + 1; j < main.threadsSize; j++) {

                                    if (thisThread.reading[inbox][j] == targetMode) {
                                        fail = true;
                                        break;
                                    } // reading if

                                }
                            }
                            if (fail) {
                                break; // stop looking at this mailbox
                            }

                            // use subfail
                            if (!fail) {
                                thisThread.reading[inbox][main.threadNum] = targetMode;


                                for (int j = main.threadsSize - 1; j > main.threadNum; j--) {
                                    if (thisThread.reading[inbox][j] == targetMode) {
                                        subfail = true;
                                        thisThread.reading[inbox][main.threadNum] = fallbackMode;
                                        break;
                                    } // reading check if
                                }
                                if (subfail) {
                                    break; // stop looking at this mailbox
                                }
                                if (!subfail) {
                                    for (int j = main.threadNum - 1; j >= 0; j--) {
                                        if (thisThread.reading[inbox][j] == targetMode) {
                                            subfail = true;
                                            thisThread.reading[inbox][main.threadNum] = fallbackMode;
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
                                        if (thisThread.reading[inbox][j] == targetMode) {
                                            subfail = true;
                                            thisThread.reading[inbox][main.threadNum] = fallbackMode;
                                            break;
                                        } // reading check if
                                    }
                                    if (subfail) {
                                        break; // stop looking at this mailbox
                                    }
                                    for (int j = threadNum + 1; j < main.threadsSize; j++) {
                                        if (thisThread.reading[inbox][j] == targetMode) {
                                            subfail = true;
                                            thisThread.reading[inbox][main.threadNum] = fallbackMode;
                                            break;
                                        } // reading check if
                                    }

                                    // reading loop
                                }
                                if (subfail) {
                                    break; // stop looking at this mailbox
                                }

                                if (!subfail) {

                                    if (logging) {
                                        System.out.println("successful send");
                                    }
                                    assert thisThread.reading[inbox][main.threadNum] == targetMode;


                                    ArrayList<AlternativeMessage> messagesToSend = messages;

                                    thisThread.inqueue.get(inbox).add(new Slice(thisThread.numSubthreads, messagesToSend, messageRate));
                                    // System.out.println("Sucessful send");
                                    thisThread.mailsize = mailsize + messagesSize;
                                    thisThread.reading[inbox][main.threadNum] = fallbackMode;

                                    removals.add(forThread);
                                    delivered += messagesSize;
                                    success = true;
                                    foundMailbox = true;


                                } else {
                                    // System.out.println("fail");
                                    thisThread.reading[inbox][main.threadNum] = fallbackMode;
                                    main.contentions++;
//                            Thread.yield();
//                        for (int m = 0; m < main.threadsSize; m++) {
//                            boolean reading = false;
//                            int t = (m + next.get()) % threadsSize;
//                            for (int j = 0; j < main.threadsSize; j++) {
//                                if (main.threads.get(t).reading[j] == targetMode) {
//                                    reading = true;
//                                    break;
//                                }
//                            }
//                            if (!reading) {
//                                for (Message message : messages) {
//                                    message.to = t;
//                                }
//                                break;
//                            }
//
//                        }
                                }

                            } // first check !fail
                            else {
                                main.contentions++;
                                thisThread.reading[inbox][main.threadNum] = fallbackMode;
//                    for (int m = 0; m < main.threadsSize; m++) {
//                        boolean reading = false;
//                        int t = (m + next.get()) % threadsSize;
//                        for (int j = 0; j < main.threadsSize; j++) {
//                            if (main.threads.get(t).reading[j] == targetMode) {
//                                reading = true;
//                                break;
//                            }
//                        }
//                        if (!reading) {
//                            for (Message message : messages) {
//                                message.to = t;
//                            }
//                            break;
//                        }
//
//                    }

                                //System.out.println("fail send");
//                        Thread.yield();
                            }

                            if (foundMailbox) {
                                break; // finished finding mailbox for this set of messages
                            }
                            if (fail || subfail) {
                                break; // skip this thread
                            }

                        } // mailboxes loop


                    for (Integer key : removals) {
                        main.outqueue.get(key).clear();
                    }
                    removals.clear();


                    // System.out.println("Successful receive");


                    Thread.yield();
                } else {
                    main.writing[main.threadNum] = NEITHER;
                }

            } // subcheck doubly safe
            else {

                main.writing[main.threadNum] = NEITHER;
            }
        } else {
            main.writing[main.threadNum] = NEITHER;
        }



        return success;
    }

    public boolean tryReceiveInbox(Actor2ParallelMessageCreation main, Run run, int inbox, int depth) {
        boolean subcheck = false;
        boolean fail = false;
        boolean success = false;
        int targetMode = 0;

        for (int j = 0; j < main.threadNum; j++) {
            if (main.reading[inbox][j] == targetMode) {
                fail = true;

                break;
            } // reading test
        }
        for (int j = main.threadNum + 1; j < main.threadsSize; j++) {
            if (main.reading[inbox][j] == targetMode) {
                fail = true;
                break;
            } // reading test
        }
        // reading loop


        if (!fail) {
            main.reading[inbox][main.threadNum] = targetMode;

            for (int j = main.threadsSize - 1; j >= 0; j--) {
                if (j != main.threadNum && main.reading[inbox][j] == targetMode) {


                    subcheck = true;
                    break;
                } // reading check
            } // reading loop

            if (!subcheck) {
                for (int j = 0; j < main.threadsSize; j++) {
                    if (j != main.threadNum && main.reading[inbox][j] == targetMode) {


                        subcheck = true;
                        break;
                    } // reading check
                } // reading loop

            }
            if (!subcheck && main.inqueue.get(inbox).size() > 0) {
                assert main.reading[inbox][main.threadNum] == targetMode;
                assert main.inqueue.get(inbox).size() > 0 : main.inqueue.get(inbox).size();
                success = true;

                Slice slice = main.inqueue.get(inbox).get(0);
                if (slice != null) {


                    if (slice.popped() == 0) {
                        main.inqueue.get(inbox).remove(0);
                    }
                    List<AlternativeMessage> subthread = slice.subthread(this.subthread);
                    main.mailsize = main.mailsize - subthread.size();
                    main.reading[inbox][main.threadNum] = NEITHER;

                    for (AlternativeMessage message : subthread) {

                        main.requestCount = main.requestCount + message.body;



                        // System.out.println(String.format("%d received %d from %d", threadNum, message.body, message.from));
                    }


                    if (main.logging) {
                        System.out.println("Successful receive");
                    }


                    Thread.yield();
                } else {
                    main.reading[inbox][main.threadNum] = NEITHER;
                }

            } // subcheck doubly safe
            else {

                main.reading[inbox][main.threadNum] = NEITHER;
//                    Thread.yield();
                main.contentions++;
                if (depth < 1) {
                    for (int m = 0; m < main.threadsSize; m++) {
                        boolean reading = false;

                        targetMode = PREEMPTED;
                        for (int j = 0; j < main.threadsSize; j++) {
                            if (main.threads.get(m).reading[inbox][j] == targetMode) {
                                reading = true;
                                break;
                            }
                        }
                        if (!reading) {
                            if (!main.threads.get(m).started) {

                                main.threads.get(m).reading[inbox][main.threadNum] = targetMode;

                                boolean innerfail = false;
                                for (int j = 0; j < main.threadsSize; j++) {
                                    if (main.threads.get(m).reading[inbox][j] == targetMode) {
                                        innerfail = true;
                                        break;
                                    }
                                }
                                if (!innerfail) {

                                    transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1, true);
                                }
                                main.threads.get(m).reading[inbox][main.threadNum] = NEITHER;
                                break;
                            }

                        }

                    }
                }

            }
        } // ^ safe to read
        else {


            main.reading[inbox][main.threadNum] = NEITHER;
//                Thread.yield();
            // System.out.println("failed to read");
            main.contentions++;
            if (depth < 1) {
                for (int m = 0; m < main.threadsSize; m++) {
                    boolean reading = false;
                    targetMode = PREEMPTED;
                    for (int j = 0; j < main.threadsSize; j++) {
                        if (main.threads.get(m).reading[inbox][j] == targetMode) {
                            reading = true;
                            break;
                        }
                    }
                    if (!reading) {
                        if (!main.threads.get(m).started) {

                            main.threads.get(m).reading[inbox][main.threadNum] = targetMode;
                            boolean innerfail = false;
                            for (int j = 0; j < main.threadsSize; j++) {
                                if (main.threads.get(m).reading[inbox][j] == targetMode) {
                                    innerfail = true;
                                    break;
                                }
                            }
                            if (!innerfail) {
                                transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1, true);
                            }
                            main.threads.get(m).reading[inbox][main.threadNum] = NEITHER;
                            break;
                        }
                    }


                }
            }


        }
        return success;
    }

    public boolean transaction(Actor2ParallelMessageCreation main, Run run, int depth, boolean send) {
        main.started = true;
        boolean preempted = false;

        for (int inbox = 0; inbox < main.mailboxes; inbox++) {
            int targetMode = PREEMPTED;
            for (int i = 0; i < main.threadsSize; i++) {
                if (main.reading[inbox][i] == targetMode) {
                    preempted = true;
                    break;
                }
            }
        }


        if (preempted) {
            // System.out.println("Preempted");
            return false;
        }
        if (send) {
            int inboxStart = (main.localNext.getAndAdd(1)) % main.mailboxes;
            for (int i = 0 ; i < threadsSize; i++) {
                if (main.outqueue.containsKey(i)) {
                    boolean success = tryConnectToThread(main, inboxStart, i);
                }
            }
            // successfully sent a message to a thread
        }

        // message loop

        // try read messages

        boolean successReceive = false;
        for (int inbox = 0; inbox < main.mailboxes; inbox++) {
            int t = (main.localNext.getAndAdd(1)) % main.mailboxes;
            if (mailsize > 0 && main.inqueue.get(t).size() > 0) {
                successReceive = tryReceiveInbox(main, run, t, 0);
            }

        }
        main.started = false;
        return successReceive;
    }

    public void run() {
        Run run = new Run(threadNum, threadNum);
        Random random = new Random();
        List<Actor2ParallelMessageCreation> rthreads = new ArrayList<>(threads);
        Collections.reverse(rthreads);

        if (subthread > 0 && !creationThread) { // sending/receiving thread
            while (running) {
                transaction(this, run, 0, false);

            }
        } else if (creationThread) { // creation thread
            while (running) {
                for (int i = 0; i < threadsSize; i++) {
                    if (outqueue.get(i).size() == 0) {
                        run.lastThread = (next.getAndAdd(1) + threadNum) % threadsSize;
                        tryCreateBatchOfMessages(this, run.lastThread, run);
                        if (logging) { System.out.println("creation thread creating"); }
                        Thread.yield();
                    }
                }
            }

        } else {
            while (running) {
                if (outqueue.get(run.lastThread).size() == 0) {
                    /**
                     there is contention writing to the same thread, so try spread it out
                     **/
                    // System.out.println(String.format("Subtread %d creating messages", subthread));
                    // System.out.println("Creating messages to send...");
                    run.lastThread = (next.getAndAdd(1) + threadNum) % threadsSize;
                    createMessagesForBatch(run, run.lastThread);

                    sent += batchSize * messageRate;
                    // System.out.println("Created messages to send...");


                } else {
                    transaction(this, run, 0, true);
                }
            }


        }
    }

    private void createMessagesForBatch(Run run, int forThread) {
        ArrayList<ArrayList<AlternativeMessage>> batches = new ArrayList<>();


        for (int t = 0 ; t < batchSize; t++) {
            ArrayList<AlternativeMessage> batch = new ArrayList<>(messageRate);
            for (int i = 0; i < messageRate; i++) {

                AlternativeMessage message = new AlternativeMessage(threadNum, forThread, 1);

                batch.add(message);


            }
            batches.add(batch);
        }
        this.outqueue.put(forThread, batches);
    }

    private void tryCreateBatchOfMessages(Actor2ParallelMessageCreation main, int forThread, Run run) {
        int fallbackMode = NEITHER;
        if (!main.outqueue.containsKey(forThread)) {
            return;
        }
        int targetMode = main.outqueue.get(forThread).size() + 1;
        int messagesSize = messages.size();
        boolean fail = false;
        boolean subfail = false;
        for (int j = 0; j < main.threadNum - 1; j++) {


            if (main.writing[j] == targetMode) {
                fail = true;
                break;
            } // reading if

        }    // reading loop
        if (fail) {
            return; // stop looking at this mailbox
        }
        if (!fail) {
            for (int j = threadNum + 1; j < main.threadsSize; j++) {

                if (main.writing[j] == targetMode) {
                    fail = true;
                    break;
                } // reading if

            }
        }
        if (fail) {
            return; // stop looking at this mailbox
        }

        // use subfail
        if (!fail) {
            main.writing[main.threadNum] = targetMode;


            for (int j = main.threadsSize - 1; j > main.threadNum; j--) {
                if (main.writing[j] == targetMode) {
                    subfail = true;
                    main.writing[main.threadNum] = fallbackMode;
                    break;
                } // reading check if
            }
            if (subfail) {
                return; // stop looking at this mailbox
            }
            if (!subfail) {
                for (int j = main.threadNum - 1; j >= 0; j--) {
                    if (main.writing[j] == targetMode) {
                        subfail = true;
                        main.writing[main.threadNum] = fallbackMode;
                        break;
                    } // reading check if
                }
            }
            // reading loop
            if (subfail) {
                return; // stop looking at this mailbox
            }
            if (!subfail) {
                for (int j = 0; j < main.threadNum; j++) {
                    if (main.writing[j] == targetMode) {
                        subfail = true;
                        main.writing[main.threadNum] = fallbackMode;
                        break;
                    } // reading check if
                }
                if (subfail) {
                    return; // stop looking at this mailbox
                }
                for (int j = threadNum + 1; j < main.threadsSize; j++) {
                    if (main.writing[j] == targetMode) {
                        subfail = true;
                        main.writing[main.threadNum] = fallbackMode;
                        break;
                    } // reading check if
                }

                // reading loop
            }
            if (subfail) {
                return; // stop looking at this mailbox
            }

            if (!subfail) {

                // System.out.println("successful send");
                assert main.writing[main.threadNum] == targetMode;

                createMessagesForBatch(run, forThread);
                if (logging) {
                    System.out.println("Successful batch create");
                }

                main.writing[main.threadNum] = fallbackMode;


                delivered += messagesSize;



            } else {
                // System.out.println("fail");
                main.writing[main.threadNum] = fallbackMode;
                main.contentions++;
//                            Thread.yield();
//                        for (int m = 0; m < main.threadsSize; m++) {
//                            boolean reading = false;
//                            int t = (m + next.get()) % threadsSize;
//                            for (int j = 0; j < main.threadsSize; j++) {
//                                if (main.threads.get(t).reading[j] == targetMode) {
//                                    reading = true;
//                                    break;
//                                }
//                            }
//                            if (!reading) {
//                                for (Message message : messages) {
//                                    message.to = t;
//                                }
//                                break;
//                            }
//
//                        }
            }

        } // first check !fail
        else {
            main.writing[main.threadNum] = fallbackMode;
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

    public static class AlternativeMessage {
        private int body;
        private int to;
        private int from;

        public AlternativeMessage(int from, int lastThread, int body) {
            this.from = from;
            this.body = body;
            this.to = lastThread;
        }
    }

    public static class Slice {
        private final int size;
        public AtomicInteger refs;
        public ArrayList<AlternativeMessage> messages;
        public int messageRate;
        public Slice(int size, ArrayList<AlternativeMessage> messages, int messageRate) {
            this.refs = new AtomicInteger(size);
            this.messageRate = messageRate;
            this.messages = messages;
            this.size = size;
        }
        public List<AlternativeMessage> subthread(int subthread) {
            int start = (subthread * (messageRate) / size);
            int end = (subthread + 1) * (messageRate / size);
            // System.out.println(String.format("From range %d to %d", start, end));
            return messages.subList(start, end);
        }
        public boolean isRetrievedEverywhere() {
            return refs.get() == 0;
        }
        public int popped() {
            return this.refs.decrementAndGet();
        }
    }
}