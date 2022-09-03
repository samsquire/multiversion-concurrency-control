package main;

import java.util.*;
import java.util.concurrent.atomic.*;


class Actor2 extends Thread {
    private int NEITHER = -1;
    private int PREEMPTED = -2;
    private final ArrayList<ArrayList<AlternativeMessage>> messages;
    private int mailboxes;
    public AtomicInteger next = new AtomicInteger(0);
    public AtomicInteger localNext = new AtomicInteger(0);
    public volatile boolean running = true;
    public int threadNum;
    private HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>> outqueue;
    public ArrayList<ArrayList<ArrayList<AlternativeMessage>>> inqueue;
    private volatile int reading[][];
    private Integer mailsize;
    public long requestCount;
    public int contentions = 0;
    private volatile boolean started;
    private int threadsSize;
    private boolean outgoing = false;
    private int sent;
    private int delivered;
    List<Integer> removals;

    public void setThreads(List<Actor2> threads) {
        this.threads = threads;
        this.threadsSize = threads.size();

        for (int i = 0 ; i < threadsSize; i++) {
            this.outqueue.put(i, new ArrayList<>(10000));
        }
        this.reading = new int[mailboxes][threadsSize];

    }


    private int messageRate;
    private List<Actor2> threads;

    public Actor2(ArrayList<ArrayList<AlternativeMessage>> messages, int messageRate, int threadNum, List<Actor2> threads, int size, boolean synchronizer, int mailboxes) {
        this.messageRate = messageRate;
        this.threads = threads;
        this.running = true;
        this.threadNum = threadNum;
        this.outqueue = new HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>>();
        this.inqueue = new ArrayList<ArrayList<ArrayList<AlternativeMessage>>>(mailboxes);
        for (int i = 0 ; i <= mailboxes; i++) {
            this.inqueue.add(new ArrayList<>());
        }
        this.mailboxes = mailboxes;
        this.messages = messages;
        this.mailsize = 0;


        this.removals = new ArrayList<>(10000);


    }

    public static void main(String[] args) throws InterruptedException {
        ArrayList<Actor2> threads = new ArrayList<>();
        int mailboxes = 10;
        int messageRate = 1000000;
        System.out.println("Creating test data...");


        ArrayList<ArrayList<AlternativeMessage>> messages = new ArrayList<>();


        for (int i = 0 ; i <= 101; i++) {
            ArrayList<AlternativeMessage> innerlist = new ArrayList<>();
            for (int k = 0; k < messageRate; k++) {
                innerlist.add(new AlternativeMessage(1));
            }
            messages.add(innerlist);
        }



        System.out.println("Created test data.");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            Actor2 thread = new Actor2(messages, messageRate, i, threads, 101, false, mailboxes);
            threads.add(thread);
        }

        Actor2 synchronizer = new Actor2(messages, messageRate, 100, new ArrayList<>(threads), 101, true, mailboxes);
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
        long totalRequests = 0;
        for (int i = 0; i < 101; i++) {
            totalRequests += threads.get(i).requestCount;
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

    public boolean tryConnectToThread(Actor2 main, int startMailbox) {
        boolean success = false;


        for (Map.Entry<Integer, ArrayList<ArrayList<AlternativeMessage>>> entry : outqueue.entrySet()) {
            boolean foundMailbox = false;
            boolean subfail = false;
            boolean fail = false;

            Actor2 thisThread = threads.get(entry.getKey());

            for (int mb = 0 ; mb < main.mailboxes; mb++) {
                int inbox = (mb + startMailbox) % main.mailboxes;

                ArrayList<ArrayList<AlternativeMessage>> messages = entry.getValue();

                int fallbackMode = -1;
                int targetMode = thisThread.inqueue.get(inbox).size() + 1;
                int messagesSize = messageRate;
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

                        // System.out.println("successful send");
                        assert thisThread.reading[inbox][main.threadNum] == targetMode;


                        thisThread.inqueue.get(inbox).add(messages.get(thisThread.threadNum));
                        // System.out.println("Sucessful send");
                        thisThread.mailsize = mailsize + messagesSize;
                        thisThread.reading[inbox][main.threadNum] = fallbackMode;

                        removals.add(entry.getKey());
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

        } // outqueue loop
        for (Integer key : removals) {
            main.outqueue.remove(key);
        }
        removals.clear();
        return success;
    }

    public boolean tryReceiveInbox(Actor2 main, Run run, int inbox, int depth) {
        boolean subcheck = false;
        boolean fail = false;
        boolean success = false;
        int targetMode = 0;
//        if (currentMode == RIGHT) {
//            targetMode = LEFT;
//        } else {
//            targetMode = RIGHT;
//        }



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
            if (!subcheck) {
                assert main.reading[inbox][main.threadNum] == targetMode;
                success = true;

                ArrayList<AlternativeMessage> messages = main.inqueue.get(inbox).get(0);
                main.inqueue.get(inbox).remove(0);
                main.mailsize =  mailsize - main.inqueue.get(0).size();
                main.reading[inbox][main.threadNum] = NEITHER;
                for (AlternativeMessage message : messages) {

                    main.requestCount = main.requestCount + message.body;


                    // System.out.println(String.format("%d received %d from %d", threadNum, message.body, message.from));
                }





                // System.out.println("Successful receive");




                Thread.yield();

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

                                    transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1);
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
                                transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1);
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

    public boolean transaction(Actor2 main, Run run, int depth) {
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

        int inboxStart = (main.localNext.getAndAdd(1)) % main.mailboxes;
        boolean success = tryConnectToThread(main, inboxStart);
        // successfully sent a message to a thread


        // message loop

        // try read messages

        boolean successReceive = false;
        for (int inbox = 0 ; inbox < main.mailboxes; inbox++) {
            int t = (main.localNext.getAndAdd(1)) % main.mailboxes;
            if (main.mailsize > 0) {
                successReceive = tryReceiveInbox(main, run, t, 0);
            }

        }
        main.started = false;
        return successReceive;
    }

    public void run() {
        Run run = new Run(threadNum, threadNum);
        Random random = new Random();
        List<Actor2> rthreads = new ArrayList<>(threads);
        Collections.reverse(rthreads);

        while (running || sent != delivered) {
            if (sent == delivered) {
                /**
                 there is contention writing to the same thread, so try spread it out
                 **/

                for (int i = 0; i < threadsSize; i++) {

                    this.outqueue.put(i, messages);

                }
                sent += messageRate * threadsSize;


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
    public static class AlternativeMessage {
        private int body;
        public AlternativeMessage(int body) {
            this.body = body;
        }
    }

}