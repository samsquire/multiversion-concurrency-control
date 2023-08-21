package main;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Interpreter extends Thread {
    private final int programStart;
    public HashMap<String, Integer> variables;
    private HashMap<String, InstructionHandler> instructionHandlers;
    private List<String> programInstructionTypes;
    public List<Map<String, String>> program;
    public boolean running = true;
    public int programCounter = 0;
    private List<Integer> stack;


    public void run() {
        Run run = new Run(threadNum, threadNum);
        Random random = new Random();
        List<Interpreter> rthreads = new ArrayList<>(threads);
        Collections.reverse(rthreads);


        while (running) {
            int pc = programStart;
            //System.out.println(String.format("program start %d", pc));
            int jump = -1;
            while (pc < program.size()) {
                String instruction = programInstructionTypes.get(pc);
                Map<String, String> parsed = program.get(pc);
                // System.out.println(String.format("%d %s %s %s", pc, instruction, parsed, variables));
                switch (instruction) {
                    case "set":
                        String variableName = parsed.get("variableName");
                        String defaultValue = parsed.get("defaultValue");

                        variables.put(variableName, Integer.parseInt(defaultValue));
                        break;
                    case "add":

                        variableName = parsed.get("variableName");
                        Integer value = variables.get(parsed.get("operandVariable"));

                        variables.put(parsed.get("operandVariable"), variables.get(variableName) + value);
                        break;
                    case "addv":

                        variableName = parsed.get("variableName");
                        Integer value2 = Integer.parseInt(parsed.get("value"));

                        variables.put(parsed.get("variableName"), variables.get(variableName) + value2);
                        break;
                    case "send":
                        ArrayList<AlternativeMessage> newMessage = new ArrayList<>();
                        newMessage.add(new AlternativeMessage(variables.get(parsed.get("sendVariableName"))));
                        Integer destination = variables.get(parsed.get("destination"));
                        // System.out.println(String.format("destination is %d", destination));
                        this.outqueue.get(parsed.get("mailboxName")).get(destination).add(newMessage);

                        boolean success = sendTransaction(this, run, 2, parsed.get("mailboxName"));
                        sent += 1;
                        break;
                    case "receive":
                        Integer received2 = transaction(this, run, 2, true, parsed.get("mailboxName"));
                        if (received2 == -1) {
                            jump = labels.get(parsed.get("failJump"));
                        } else {
                            variables.put(parsed.get("variableName"), received2);
                        }

                        break;
                    case "while":
                        if (variables.get(parsed.get("variableName")) != 1) {
                            jump = labels.get(parsed.get("jump"));
                        }
                        break;
                    case "endwhile":
                        jump = labels.get(parsed.get("jump"));
                        break;
                    case "modulo":
                        String variableName2 = parsed.get("variableName");
                        int newValue = variables.get(variableName2) % Integer.parseInt(parsed.get("amount"));
                        variables.put(variableName2, newValue);
                        break;
                    case "return":
                        jump = stack.remove(0);
                        break;
                    case "sendcode":

                        ArrayList<AlternativeMessage> newMessage2 = new ArrayList<>();
                        newMessage2.add(new AlternativeMessage(labels.get(parsed.get("sendLabel"))));
                        Integer destination2 = variables.get(parsed.get("destination"));
                        // System.out.println(String.format("destination is %d", destination));
                        this.outqueue.get(parsed.get("mailboxName")).get(destination2).add(newMessage2);

                        boolean success2 = sendTransaction(this, run, 2, parsed.get("mailboxName"));
                        sent += 1;
                        break;
                    case "receivecode":
                        stack.add(pc + 1);
                        jump = transaction(this, run, 2, true, parsed.get("mailboxName"));
                        break;
                    case "mailbox":
                        createMailbox(parsed.get("mailboxName"));
                        break;
                    case "println":
                        System.out.println(variables.get(parsed.get("variableName")));
                        break;
                    case "jump":
                        jump = labels.get(parsed.get("jumpDestination"));
                    }


                    pc++;
                    if (jump != -1) {
                        pc = jump;
                        jump = -1;
                    }
                }


            }

        }



    private int NEITHER = -1;
    private int PREEMPTED = -2;
    private final ArrayList<ArrayList<AlternativeMessage>> messages;
    private int mailboxes;
    public AtomicInteger next = new AtomicInteger(0);
    public AtomicInteger localNext = new AtomicInteger(0);
    public int threadNum;
    private HashMap<String, HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>>> outqueue;
    public HashMap<String, ArrayList<ArrayList<Slice>>> inqueue;
    private volatile int reading[][];
    private Integer mailsize;
    public long requestCount;
    public int contentions = 0;
    private volatile boolean started;
    private int threadsSize;
    private int sent;
    private int delivered;
    List<Integer> removals;
    private int subthread;
    private int receiveThreadNum;
    private int numSubthreads;

    public void setThreads(List<Interpreter> threads) {
        this.threads = threads;
        this.threadsSize = threads.size();


        this.reading = new int[mailboxes][threadsSize];
        for (int i = 0; i < mailboxes; i++) {
            for (int t = 0; t < threadsSize; t++) {
                this.reading[i][t] = NEITHER;
            }
        }

    }


    private int messageRate;
    private List<Interpreter> threads;
    private Map<String, Integer> labels;

    public void createMailbox(String mailboxName) {
        this.outqueue.put(mailboxName, new HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>>());
        this.inqueue.put(mailboxName, new ArrayList<ArrayList<Slice>>(threadsSize));
        for (int i = 0; i < threadsSize; i++) {
            this.outqueue.get(mailboxName).put(i, new ArrayList<>(10000));
        }
        for (int i = 0; i < threadsSize; i++) {
            this.inqueue.get(mailboxName).add(new ArrayList<>());
        }
    }

    public Interpreter(ArrayList<ArrayList<AlternativeMessage>> messages,
                       int subthread,
                       int messageRate,
                       int threadNum,
                       int receiveThreadNum,
                       List<Interpreter> threads,
                       int size,
                       boolean synchronizer,
                       int mailboxes,
                       int numSubthreads,
                       List<String> programInstructionTypes,
                       List<Map<String, String>> program,
                       int programStart, HashMap<String,
                                   Integer> variables,
                       Map<String, Integer> labels
    ) {
        this.programInstructionTypes = programInstructionTypes;
        this.program = program;
        this.programStart = programStart;
        this.variables = new HashMap<>(variables);
        this.subthread = subthread;
        this.numSubthreads = numSubthreads;
        this.messageRate = messageRate;
        this.threads = threads;
        this.labels = labels;
        this.running = true;
        this.threadNum = threadNum;
        this.receiveThreadNum = receiveThreadNum;


        this.mailboxes = mailboxes;
        this.messages = messages;
        this.mailsize = 0;
        this.stack = new ArrayList<>();

        this.removals = new ArrayList<>(10000);
        this.outqueue = new HashMap<>();
        this.inqueue = new HashMap<>();


    }

    private void subthreadOf(Interpreter Interpreter) {
        this.inqueue = Interpreter.inqueue;
    }




    public boolean tryConnectToThread(Interpreter main, int startMailbox, HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>> outqueue, String mailboxName) {
        boolean success = false;


        for (Map.Entry<Integer, ArrayList<ArrayList<AlternativeMessage>>> entry : outqueue.entrySet()) {
            boolean foundMailbox = false;
            boolean subfail = false;
            boolean fail = false;

            Interpreter thisThread = threads.get(entry.getKey());

            for (int mb = 0; mb < main.mailboxes; mb++) {
                int inbox = (mb + startMailbox) % main.mailboxes;

                ArrayList<ArrayList<AlternativeMessage>> messages = entry.getValue();
                if (messages.size() == 0) {
                    break;
                }

                int fallbackMode = -1;
                if (!thisThread.inqueue.containsKey(mailboxName) || thisThread.inqueue.get(mailboxName).size() <= inbox) {
                    // not initialised yet
                    continue;
                }
                int targetMode = thisThread.inqueue.get(mailboxName).get(inbox).size() + 1;
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


                        ArrayList<AlternativeMessage> messagesToSend = messages.get(0);

                        thisThread.inqueue.get(mailboxName).get(inbox).add(new Slice(thisThread.numSubthreads, messagesToSend, messageRate));
                        // System.out.println("Sucessful send");
                        thisThread.mailsize = thisThread.mailsize + messagesSize;
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
            main.outqueue.get(mailboxName).get(key).clear();
        }
        removals.clear();
        return success;
    }

    public int tryReceiveInbox(Interpreter main, Run run, int inbox, int depth, String mailboxName) {
        boolean subcheck = false;
        boolean fail = false;
        boolean success = false;
        int targetMode = 0;
        int returnValue = -1;
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

                Slice slice = main.inqueue.get(mailboxName).get(inbox).get(0);
                if (slice != null) {
                    if (slice.popped() == 0) {
                        main.inqueue.get(mailboxName).get(inbox).remove(0);
                    }
                    List<AlternativeMessage> subthread = slice.subthread(this.subthread);
                    main.mailsize = main.mailsize - subthread.size();
                    main.reading[inbox][main.threadNum] = NEITHER;

                    returnValue = subthread.get(0).body;


                    // System.out.println("Successful receive");


                    Thread.yield();
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

                                    transaction(main.threads.get(m),
                                            new Run(threads.get(m).threadNum, threads.get(m).threadNum),
                                            depth + 1, true, mailboxName);
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
                                transaction(main.threads.get(m), new Run(threads.get(m).threadNum, threads.get(m).threadNum), depth + 1, true, mailboxName);
                            }
                            main.threads.get(m).reading[inbox][main.threadNum] = NEITHER;
                            break;
                        }
                    }


                }
            }


        }
        return returnValue;
    }

    public boolean sendTransaction(Interpreter main, Run run, int depth, String mailboxName) {
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
            boolean success = tryConnectToThread(main, inboxStart, outqueue.get(mailboxName), mailboxName);
            // successfully sent a message to a thread
        return success;
    }

    public Integer transaction(Interpreter main, Run run, int depth, boolean send, String mailboxName) {
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
            return -1;
        }


        // message loop

        // try read messages

        boolean successReceive = false;
        Integer received = -1;
        for (int inbox = 0; inbox < main.mailboxes; inbox++) {
            int t = (main.localNext.getAndAdd(1)) % main.mailboxes;
            if (main.inqueue.get(mailboxName).get(t).size() > 0) {
               received = tryReceiveInbox(main, run, t, 0, mailboxName);
                break;
            }

        }
        main.started = false;
        return received;
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
            return messages;
        }

        public boolean isRetrievedEverywhere() {
            return refs.get() == 0;
        }

        public int popped() {
            return this.refs.decrementAndGet();
        }
    }
}
