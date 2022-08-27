package main;

import java.util.*;
import java.util.concurrent.atomic.*;


class Main extends Thread {
    public AtomicInteger next = new AtomicInteger(0);
    public volatile boolean running = true;
    public int threadNum;
    private List<Message> outqueue;
    public List<Message> inqueue;
    private volatile boolean reading[];
    public int requestCount;
    public int contentions = 0;
    private volatile boolean started;

    public void setThreads(List<Main> threads) {
        this.threads = threads;
    }


    private List<Main> threads;
    public Main(int threadNum, List<Main> threads, int size, boolean synchronizer) {
        this.threads = threads;
        this.running = true;
        this.threadNum = threadNum;
        this.outqueue = new ArrayList<>();
        this.inqueue = new ArrayList<>();
        this.reading = new boolean[size];
    }

    public static void main(String[] args) throws InterruptedException {
        ArrayList<Main> threads = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            Main thread = new Main(i, threads, 101, false);
            threads.add(thread);
        }
        Main synchronizer = new Main(100, new ArrayList<>(threads), 101, true);

        threads.add(synchronizer);

        for (int i = 0 ; i < 100; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));

        }
        for (int i = 0 ; i < 100 ; i++) {
            threads.get(i).start();
        }
        synchronizer.start();

        int benchmarkTime = 5000;
        Thread.sleep(benchmarkTime);
        for (int i = 0 ; i < 100 ; i++) {
            threads.get(i).running = false;
        }
        threads.get(100).running = false;
        for (int i = 0 ; i < 100; i++) {
            threads.get(i).join();
        }
        threads.get(100).join();
        int totalRequests = 0;
        for (int i = 0 ; i < 100; i++) {
            totalRequests += threads.get(i).requestCount;
        }
        long end = System.currentTimeMillis();
        long seconds = (end - start) / 1000;
        System.out.println(String.format("%d total requests", totalRequests));
        System.out.println(String.format("%d requests per second", totalRequests / seconds));
        System.out.println(String.format("Time taken: %d", seconds));
        int contentions = 0;
        for (int i = 0 ; i < threads.size(); i++) {
            contentions += threads.get(i).contentions;
        }
        System.out.println(String.format("Contentions: %d", contentions));

    }

    public void transaction(Main main, Run run, int depth) {

        boolean preempted = false;
        for (int i = 0 ; i < main.threads.size(); i++) {
            if (main.reading[i]) {
                preempted = true;
                break;
            }
        }

        if (preempted) {

            return;
        }
        main.started = true;
        for (int i = 0; i < main.outqueue.size(); i++) {
            Message message = main.outqueue.get(i);
            assert message != null;
            boolean fail = false;
            for (int j = 0; j < main.threads.size(); j++) {

                if (j != main.threadNum && main.threads.get(message.to).reading[j]) {
                    fail = true;
                    break;
                } // reading if

            }    // reading loop

            boolean subfail = false;
            if (!fail) {
                main.threads.get(message.to).reading[main.threadNum] = true;


                for (int j = main.threads.size() - 1; j >= 0; j--) {
                    if (j != main.threadNum && main.threads.get(message.to).reading[j]) {
                        subfail = true;
                        main.threads.get(message.to).reading[main.threadNum] = false;
                        break;
                    } // reading check if
                } // reading loop

                if (!subfail) {
                    for (int j = 0; j < main.threads.size(); j++) {
                        if (j != main.threadNum && main.threads.get(message.to).reading[j]) {
                            subfail = true;
                            main.threads.get(message.to).reading[main.threadNum] = false;
                            break;
                        } // reading check if
                    } // reading loop
                }

                if (!subfail) {
                    // System.out.println("successful send");
                    assert main.threads.get(message.to).reading[main.threadNum] == true;
                    main.threads.get(message.to).inqueue.add(message);
                    main.outqueue.remove(message);
                    main.threads.get(message.to).reading[main.threadNum] = false;

                } else {
                    // System.out.println("fail");
                    main.threads.get(message.to).reading[main.threadNum] = false;
                    main.contentions++;
//                            Thread.yield();
                    for (int m = 0; m < main.threads.size(); m++) {
                        boolean reading = false;
                        for (int j = 0; j < main.threads.size(); j++) {
                            if (main.threads.get(m).reading[j]) {
                                reading = true;
                                break;
                            }
                        }
                        if (!reading) {
                            message.to = m;
                            break;
                        }

                    }
                }

            } // first check !fail
            else {
                main.contentions++;
                main.threads.get(message.to).reading[message.from] = false;
                for (int m = 0; m < main.threads.size(); m++) {
                    boolean reading = false;
                    for (int j = 0; j < main.threads.size(); j++) {
                        if (main.threads.get(m).reading[j]) {
                            reading = true;
                            break;
                        }
                    }
                    if (!reading) {
                        message.to = m;
                        break;
                    }

                }

                //System.out.println("fail send");
//                        Thread.yield();
            }


        } // message loop

// try read messages

        boolean subcheck = false;
        boolean fail = false;

        for (int j = 0; j < main.threads.size(); j++) {
            if (j != main.threadNum && main.reading[j]) {
                fail = true;
                break;
            } // reading test
        } // reading loop


        if (!fail) {

            main.reading[main.threadNum] = true;


            for (int j = main.threads.size() - 1; j >= 0; j--) {
                if (j != main.threadNum && main.reading[j]) {


                    subcheck = true;
                    break;
                } // reading check
            } // reading loop

            if (!subcheck) {
                for (int j = 0; j < main.threads.size(); j++) {
                    if (j != main.threadNum && main.reading[j]) {


                        subcheck = true;
                        break;
                    } // reading check
                } // reading loop
            }
            if (!subcheck) {
                assert main.reading[main.threadNum] == true;


                for (Message message : main.inqueue) {

                    main.requestCount++;
                    // System.out.println(String.format("%d received %d from %d", threadNum, message.body, message.from));
                }

                main.inqueue.clear();
                main.reading[main.threadNum] = false;
                Thread.yield();

            } // subcheck doubly safe
            else {
                main.reading[main.threadNum] = false;
//                    Thread.yield();
                main.contentions++;
                if (depth < 1) {
                    for (int m = 0; m < main.threads.size(); m++) {
                        boolean reading = false;
                        for (int j = 0; j < main.threads.size(); j++) {
                            if (main.threads.get(m).reading[j]) {
                                reading = true;
                                break;
                            }
                        }
                        if (!reading) {
                            if (!main.threads.get(m).started) {

                                main.threads.get(m).reading[main.threadNum] = true;

                                boolean innerfail = false;
                                for (int j = 0; j < main.threads.size(); j++) {
                                    if (main.threads.get(m).reading[j]) {
                                        innerfail = true;
                                        break;
                                    }
                                }
                                if (!innerfail) {

                                    transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1);
                                }
                                main.threads.get(m).reading[main.threadNum] = false;
                                break;
                            }

                        }

                    }
                }

            }
        } // ^ safe to read
        else {


            main.reading[main.threadNum] = false;
//                Thread.yield();
            // System.out.println("failed to read");
            main.contentions++;
            if (depth < 1) {
                for (int m = 0; m < main.threads.size(); m++) {
                    boolean reading = false;
                    for (int j = 0; j < main.threads.size(); j++) {
                        if (main.threads.get(m).reading[j]) {
                            reading = true;
                            break;
                        }
                    }
                    if (!reading) {
                        if (!main.threads.get(m).started) {

                            main.threads.get(m).reading[main.threadNum] = true;
                            boolean innerfail = false;
                            for (int j = 0; j < main.threads.size(); j++) {
                                if (main.threads.get(m).reading[j]) {
                                    innerfail = true;
                                    break;
                                }
                            }
                            if (!innerfail) {          transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1);
                            }                       main.threads.get(m).reading[main.threadNum] = false;
                            break;
                        }
                    }



                }
            }


        }
        main.started = false;
    }

    public void run() {
        Run run = new Run(threadNum, threadNum);
        Random random = new Random();
        List<Main> rthreads = new ArrayList<>(threads);
        Collections.reverse(rthreads);
        while (running || outqueue.size() > 0) {
            if (outqueue.size() == 0) {
                /**
                 there is contention writing to the same thread, so try spread it out
                 **/
                run.lastThread = (next.getAndAdd(1) + threadNum) % threads.size();

                for (int i = 0 ; i < 1; i++) {
                    if (run.lastThread == threadNum) {
                        run.lastThread = (run.lastThread + 1) % threads.size();
                    }
                    Message message = new Message(threadNum, run.lastThread, run.counter++);
                    this.outqueue.add(message);
                }

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