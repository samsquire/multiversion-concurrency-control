package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ReferencePassing3 extends Thread {
    private int NEITHER = -1;
    private int PREEMPTED = -2;
    private volatile int reading[][];
    private List<ReferencePassing3> threads;
    private boolean running = true;
    private long count = 0;
    private DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
    public List<List<DoublyLinkedList>> incoming;
    private List<ReferencePassing3> owner = new ArrayList<>();
    private int threadNum;
    private int mailboxSize;
    private int threadsSize;

    public ReferencePassing3(int threadNum, int mailboxSize) {
        this.threadNum = threadNum;
        this.mailboxSize = mailboxSize;
    }

    public ReferencePassing3(int threadNum) {
        this.threadNum = threadNum;
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        int mailboxSize = 10;
        List<ReferencePassing3> threads = new ArrayList<>();
        for (int i = 0 ; i < threadCount; i++) {
            threads.add(new ReferencePassing3(i, mailboxSize));
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
        }

        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).start();
        }
        long start = System.currentTimeMillis();

        Thread.sleep(5000);
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).running = false;
        }
        for (int i = 0 ; i < threadCount; i++) {
            threads.get(i).join();
        }
        System.out.println("Finished");
        long end = System.currentTimeMillis();

        long totalRequests = 0;

        for (int i = 0; i < threads.size(); i++) {
            totalRequests += threads.get(i).count;
        }

        double seconds = (end - start) / 1000.0;

        System.out.println(String.format("%d total requests", totalRequests));
        double l = totalRequests / seconds;
        System.out.println(String.format("%f requests per second", l));
        System.out.println(String.format("Time taken: %f", seconds));
    }

    private void setThreads(ArrayList<ReferencePassing3> threads) {
        this.threads = threads;
        this.threadsSize = threads.size();
        this.reading = new int[10][threads.size()];
        this.incoming = new ArrayList<>();
        for (int i = 0 ; i < 10; i++) {
            this.reading[i] = new int[threads.size()];
            Arrays.fill(this.reading[i], NEITHER);
            this.incoming.add(new ArrayList<>());
        }
        for (int i = 0 ; i < threadsSize; i++) {
            this.owner.add(null);

        }
    }

    public void run() {
        Random rng = new Random();
        while (running) {

                for (int inbox = 0; inbox < 10; inbox++) {
                    if (incoming.get(inbox).size() > 0) {
                        int fallbackMode = -1;
                        boolean fail = false;
                        boolean subfail = false;
                        int targetMode = 0;
                        for (int j = 0; j < threadNum - 1; j++) {


                            if (reading[inbox][j] == targetMode) {
                                fail = true;
                                break;
                            } // reading if

                        }    // reading loop
                        if (fail) {
                            break; // stop looking at this mailbox
                        }
                        if (!fail) {
                            for (int j = threadNum + 1; j < threadsSize; j++) {

                                if (reading[inbox][j] == targetMode) {
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
                            reading[inbox][threadNum] = targetMode;


                            for (int j = threadsSize - 1; j > threadNum; j--) {
                                if (reading[inbox][j] == targetMode) {
                                    subfail = true;
                                    reading[inbox][threadNum] = fallbackMode;
                                    break;
                                } // reading check if
                            }
                            if (subfail) {
                                break; // stop looking at this mailbox
                            }
                            if (!subfail) {
                                for (int j = threadNum - 1; j >= 0; j--) {
                                    if (reading[inbox][j] == targetMode) {
                                        subfail = true;
                                        reading[inbox][threadNum] = fallbackMode;
                                        break;
                                    } // reading check if
                                }
                            }
                            // reading loop
                            if (subfail) {
                                break; // stop looking at this mailbox
                            }
                            if (!subfail) {
                                for (int j = 0; j < threadNum; j++) {
                                    if (reading[inbox][j] == targetMode) {
                                        subfail = true;
                                        reading[inbox][threadNum] = fallbackMode;
                                        break;
                                    } // reading check if
                                }
                                if (subfail) {
                                    break; // stop looking at this mailbox
                                }
                                for (int j = threadNum + 1; j < threadsSize; j++) {
                                    if (reading[inbox][j] == targetMode) {
                                        subfail = true;
                                        reading[inbox][threadNum] = fallbackMode;
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
                                assert reading[inbox][threadNum] == targetMode;
                                incoming.get(inbox).get(0)
                                        .insert(incoming.get(inbox).get(0).value + 1);
                                count++;
                                owner.get(inbox).data = incoming.get(inbox).get(0);
                                reading[inbox][threadNum] = fallbackMode;
    //                System.out.println("Returning reference to other thread");
                                break;

                            } else {
                                // System.out.println("fail");
                                reading[inbox][threadNum] = fallbackMode;
    //
                            }

                        } // first check !fail
                        else {
                            reading[inbox][threadNum] = fallbackMode;
                        }
    //                System.out.println("Receiving another thread's data");
                    }

            }
            if (data == null) {
                continue;
            }
            ReferencePassing3 other = threads.get(rng.nextInt(threads.size()));
            for (int inbox = 0; inbox < 10; inbox++) {

                int fallbackMode = -1;
                boolean fail = false;
                boolean subfail = false;
                int targetMode = 0;
                for (int j = 0; j < threadNum - 1; j++) {


                    if (reading[inbox][j] == targetMode) {
                        fail = true;
                        break;
                    } // reading if

                }    // reading loop
                if (fail) {
                    break; // stop looking at this mailbox
                }
                if (!fail) {
                    for (int j = threadNum + 1; j < threadsSize; j++) {

                        if (reading[inbox][j] == targetMode) {
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
                    reading[inbox][threadNum] = targetMode;


                    for (int j = threadsSize - 1; j > threadNum; j--) {
                        if (reading[inbox][j] == targetMode) {
                            subfail = true;
                            reading[inbox][threadNum] = fallbackMode;
                            break;
                        } // reading check if
                    }
                    if (subfail) {
                        break; // stop looking at this mailbox
                    }
                    if (!subfail) {
                        for (int j = threadNum - 1; j >= 0; j--) {
                            if (reading[inbox][j] == targetMode) {
                                subfail = true;
                                reading[inbox][threadNum] = fallbackMode;
                                break;
                            } // reading check if
                        }
                    }
                    // reading loop
                    if (subfail) {
                        break; // stop looking at this mailbox
                    }
                    if (!subfail) {
                        for (int j = 0; j < threadNum; j++) {
                            if (reading[inbox][j] == targetMode) {
                                subfail = true;
                                reading[inbox][threadNum] = fallbackMode;
                                break;
                            } // reading check if
                        }
                        if (subfail) {
                            break; // stop looking at this mailbox
                        }
                        for (int j = threadNum + 1; j < threadsSize; j++) {
                            if (reading[inbox][j] == targetMode) {
                                subfail = true;
                                reading[inbox][threadNum] = fallbackMode;
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
                        assert reading[inbox][threadNum] == targetMode;
                        other.owner.set(inbox, this);
                        other.incoming.get(inbox).add(data);
                        data = null;
                        reading[inbox][threadNum] = fallbackMode;
                        count++;
                        break;
//                System.out.println("Returning reference to other thread");


                    } else {
                        // System.out.println("fail");
                        reading[inbox][threadNum] = fallbackMode;
//
                    }

                } // first check !fail
                else {
                    reading[inbox][threadNum] = fallbackMode;
                }
            }
        }
    }


}
