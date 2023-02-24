package main;



import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageInterpreter extends Thread {
    private final int programStart;
    public final HashMap<String, Map<String, Object>> mapvariables;
    private Pattern pattern;
    private final AST parsedProgram;
    private Matcher matcher;
    public Map<String, Integer> intvariables;
    public Map<String, String> stringvariables;
    private Map<String, InstructionHandler> instructionHandlers;
    private List<String> programInstructionTypes;
    public String program;
    public volatile boolean running = true;
    public int programCounter = 0;
    private List<Integer> stack;
    private String programString;
    private int pos;
    private boolean end;
    private char last_char;
    private Map<String, VariableDeclaration> types;
    private List<Integer> intstack;
    private List<String> stringstack;
    private List<Integer> argumentstack;
    private List<String> stringarguments;

    public LanguageInterpreter(AST parsedProgram,
                               ArrayList<ArrayList<AlternativeMessage>> messages,
                               int subthread,
                               int messageRate,
                               int threadNum,
                               int i,
                               ArrayList<Object> objects,
                               int totalSize,
                               boolean synchronizer,
                               int mailboxes,
                               int numSubthreads,
                               List<String> programInstructionTypes,
                               String programString,
                               int programStart,
                               HashMap<String, Integer> variables,
                               Map<String, Integer> labels,
                               Map<String, String> stringvariables,
                               Map<String, Integer> intvariables) {
        this.programInstructionTypes = programInstructionTypes;
        this.intvariables = intvariables;
        this.program = program;
        this.programStart = programStart;
        this.stringvariables = stringvariables;
        this.mapvariables = new HashMap<>();
        this.subthread = subthread;
        this.numSubthreads = numSubthreads;
        this.messageRate = messageRate;
        this.threads = threads;
        this.labels = labels;
        this.running = true;
        this.threadNum = threadNum;
        this.receiveThreadNum = receiveThreadNum;
        this.parsedProgram = parsedProgram;

        this.mailboxes = mailboxes;
        this.messages = messages;
        this.mailsize = 0;
        this.stack = new ArrayList<>();

        this.removals = new ArrayList<>(10000);
        this.outqueue = new HashMap<>();
        this.inqueue = new HashMap<>();
        this.types = new HashMap<>();
        this.stringstack = new ArrayList<>();
        this.intstack = new ArrayList<>();
        this.argumentstack = new ArrayList<>();
        this.stringarguments = new ArrayList<>();

    }



    public void run() {
        Run run = new Run(threadNum, threadNum);
        Random random = new Random();
        List<LanguageInterpreter> rthreads = new ArrayList<>(threads);
        Collections.reverse(rthreads);

        System.out.println("PARSED PROGRAM");
        System.out.println(parsedProgram);

//        CodeSegment codegen = new CodeSegment(new ArrayList<String>(), new ArrayList<Map<String, String>>());
        CodeSegment codegen = parsedProgram.codegen();
        System.out.println(codegen.instructions);
        System.out.println(codegen.parsed);
        assert codegen.instructions.size() == codegen.parsed.size() : codegen.parsed.size();
        for (int i = 0 ; i < codegen.instructions.size(); i++) {
            String instruction = codegen.instructions.get(i);
            Map<String, String> parsed = codegen.parsed.get(i);
            if (instruction.equals("createlabel")) {
                labels.put(parsed.get("label"), i);
            }
            if (instruction.equals("define")) {
                System.out.println(String.format("Defining variable %s", instruction));
                types.put(parsed.get("variable"), new VariableDeclaration(parsed.get("variable"), parsed, parsed.get("type")));
            }
            if (instruction.equals("load") || instruction.equals("store")) {
                String variable = parsed.get("variable");
                System.out.println(String.format("Resolving type for variable %s", variable));
                parsed.put("type", types.get(variable).type);
            }
            if (instruction.equals("set")) {
                String variable = parsed.get("variableName");
                parsed.put("type", types.get(variable).type);
            }
        }
        System.out.println("generated instructions:");
        for (int x = 0 ; x < codegen.instructions.size(); x++) {
            System.out.println(String.format("%d %s %s", x, codegen.instructions.get(x), codegen.parsed.get(x)));
        };
        System.out.println(labels);
        List<Map<String, Object>> mapstack = new ArrayList<>();
        List<String> typestack = new ArrayList<>();
        List<String> mapassign = new ArrayList<>();
        List<LValue> lvalue = new ArrayList<>();
        int pc = programStart;
        stringarguments.add("1");
        argumentstack.add(100);

        stack.add(46);
        while (running) {

            //System.out.println(String.format("program start %d", pc));
            int jump = -1;
            while (running && pc < codegen.instructions.size()) {
//                System.out.println("Running");
                String instruction = codegen.instructions.get(pc);
                Map<String, String> parsed = codegen.parsed.get(pc);
                // System.out.println(String.format("%d %s %s %s", pc, instruction, parsed, variables));

                switch (instruction) {
                    case "poptype":
                        typestack.remove(typestack.size() - 1);
                        break;
                    case "popstruct":
                        mapstack.remove(0);
                        break;
                    case "define":
                        switch (parsed.get("type")) {
                            case "int":
                                Integer value5 = argumentstack.remove(0);
                                intvariables.put(parsed.get("variable"), value5);
                                break;
                            case "string":
                                String value6 = stringarguments.remove(0);
                                stringvariables.put(parsed.get("variable"), value6);
                                break;
                        }
                        System.out.println(String.format("%d DEFINE", pc));
                        break;
                    case "pushtype":
                        typestack.add(parsed.get("type"));
                        break;
                    case "pushstring":

                        stringstack.add(parsed.get("token"));
                        break;
                    case "pushargumentstr":
                        System.out.println("Pushing string argument");
                        stringarguments.add(parsed.get("argument"));
                        break;
                    case "pushint":

                        intstack.add(Integer.valueOf(parsed.get("token")));
                        break;
                    case "pushstruct":
                        HashMap<String, Object> e = new HashMap<>();
                        mapstack.add(e);
                        mapvariables.put(parsed.get("variable"), e);
                        break;
                    case "pushkey":
                        mapassign.add(stringstack.remove(stringstack.size() - 1));
                        break;
                    case "pushvalue":

                        String key = mapassign.remove(mapassign.size() - 1);
                        String type = typestack.remove(typestack.size() - 1);
                        System.out.println(String.format("Stack Type %s", type));
                        Object value;
                        switch (type) {
                            case "int":
                                int value3 = intstack.remove(intstack.size() - 1);
                                mapstack.get(mapstack.size() - 1).put(key, value3);
                                System.out.println(String.format("Map set %s int %d", key, value3));
                                break;
                            case "string":
                                System.out.println("Map set string");
                                value = stringstack.remove(stringstack.size() - 1);
                                mapstack.get(mapstack.size() - 1).put(key, value);
                                System.out.println(String.format("Map set %s %s", key, value));
                                break;
                            case "struct":
                                System.out.println("Pushing mapstack");
                                value = mapstack.remove(mapstack.size() - 1);
                                mapstack.get(mapstack.size() - 1).put(key, value);
                                System.out.println(String.format("Map set %s %s", key, value));
                                break;
                        }


                        break;
                    case "store":
//                        System.out.println(String.format("CALLING STORE %s %s", parsed.get("type"), parsed));
                        switch (parsed.get("type")) {
                            case "integer":
                                intvariables.put(parsed.get("variable"), intstack.get(intstack.size() - 1));
                                break;
                            case "struct":
                                System.out.println(String.format("Map stack size is %d", mapstack.size() - 1));
                                mapvariables.put(parsed.get("variable"), mapstack.get(mapstack.size() - 1));
                                break;
                        }
                        break;
                    case "loadhash":
                        String key2 = stringstack.remove(stringstack.size() - 1);
                        Map<String, Object> remove1 = mapstack.remove(mapstack.size() - 1);
                        Object value3 = remove1.get(key2);
                        System.out.println(String.format("Fetched %s %s", key2, value3));
                        if (remove1 instanceof HashMap) {
                            mapstack.add((HashMap<String, Object>)remove1);
                            lvalue.add(new LValue("hash", remove1, key2));
                        }
                        if (value3 instanceof String) {
                            stringstack.add((String) value3);
                            lvalue.add(new LValue("variable", remove1, key2));
                        }

                        break;
                    case "loadhashvar":
                        String key3 = stringstack.remove(stringstack.size() - 1);
                        String var = stringvariables.get(key3);
                        Map<String, Object> fetched2 = (Map<String, Object>) mapstack.remove(mapstack.size() - 1).get(var);
                        System.out.println(String.format("Fetched %s %s %s", var, key3, fetched2));
                        mapstack.add(fetched2);
                        break;
                    case "pushargument":
                        System.out.println("pushing argument");
                        argumentstack.add(Integer.valueOf(parsed.get("argument")));
                        break;
                    case "load":
                        types.get(parsed.get("token"));
                        String token1 = parsed.get("token");
                        String type2 = parsed.get("type");
                        switch (type2) {
                            case "struct":
                                Map<String, Object> token = mapvariables.get(token1);
                                mapstack.add(token);
                                break;
                            case "int":
                                intstack.add(intvariables.get(parsed.get("variable")));
                                break;
                        }
                        break;
                    case "pluseq":
                        LValue remove = lvalue.remove(lvalue.size() - 1);
                        remove.add(intstack.remove(intstack.size() - 1));
                        break;
                    case "set":
                        String variableName = parsed.get("variableName");
                        String defaultValue = parsed.get("defaultValue");
                        type = parsed.get("type");

                        switch (type) {
                            case "int":
                                intvariables.put(variableName, Integer.parseInt(defaultValue));
                                break;
                            case "struct":
                                mapvariables.put(variableName, mapstack.remove(0));
                                break;

                        }
                        break;
                    case "add":

                        variableName = parsed.get("variableName");
                        Integer intvalue = intvariables.get(parsed.get("operandVariable"));

                        intvariables.put(parsed.get("operandVariable"), intvariables.get(variableName) + intvalue);
                        break;
                    case "addv":

                        variableName = parsed.get("variableName");
                        Integer value2 = Integer.parseInt(parsed.get("value"));

                        intvariables.put(parsed.get("variableName"), intvariables.get(variableName) + value2);
                        break;
                    case "send":
                        ArrayList<AlternativeMessage> newMessage = new ArrayList<>();
                        newMessage.add(new AlternativeMessage(intvariables.get(parsed.get("sendVariableName"))));
                        Integer destination = intvariables.get(parsed.get("destination"));
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
                            intvariables.put(parsed.get("variableName"), received2);
                        }

                        break;
                    case "push":
                        // intstack.add(Integer.valueOf(parsed.get("variable")));
                        break;
                    case "loadhashin":
                        mapstack.get(intstack.remove(0));
                    case "loadhashr":
                        mapstack.add(mapstack.get(intstack.remove(0)));
                        break;
                    case "while":
                        if (intvariables.get(parsed.get("variableName")) != 1) {
                            jump = labels.get(parsed.get("jump"));
                        }
                        break;
                    case "endwhile":
                        jump = labels.get(parsed.get("jump"));
                        break;
                    case "modulo":
                        String variableName2 = parsed.get("variableName");
                        int newValue = intvariables.get(variableName2) % Integer.parseInt(parsed.get("amount"));
                        intvariables.put(variableName2, newValue);
                        break;
                    case "return":
                        jump = stack.remove(stack.size() - 1);
                        System.out.println(String.format("%d RETURN %d", pc, jump));
                        break;
                    case "call":
                        stack.add(pc + 1);
                        System.out.println("CALLING METHOD");
                        jump = labels.get(parsed.get("method"));
                        break;
                    case "sendcode":

                        ArrayList<AlternativeMessage> newMessage2 = new ArrayList<>();
                        newMessage2.add(new AlternativeMessage(labels.get(parsed.get("sendLabel"))));
                        Integer destination2 = intvariables.get(parsed.get("destination"));
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
                        System.out.println(intvariables.get(parsed.get("variableName")));
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

                if (pc >= programInstructionTypes.size()) {
                    break;
                }
            }
        System.out.println(mapstack);
        System.out.println(argumentstack);

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

    public void setThreads(List<LanguageInterpreter> threads) {
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
    private List<LanguageInterpreter> threads;
    private Map<String, Integer> labels;

    public void createMailbox(String mailboxName) {
        this.outqueue.put(mailboxName, new HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>>());
        this.inqueue.put(mailboxName, new ArrayList<ArrayList<Slice>>(mailboxes));
        for (int i = 0; i < threadsSize; i++) {
            this.outqueue.get(mailboxName).put(i, new ArrayList<>(10000));
        }
        for (int i = 0; i <= mailboxes; i++) {
            this.inqueue.get(mailboxName).add(new ArrayList<>());
        }
    }



    private void subthreadOf(LanguageInterpreter Interpreter) {
        this.inqueue = Interpreter.inqueue;
    }




    public boolean tryConnectToThread(LanguageInterpreter main, int startMailbox, HashMap<Integer, ArrayList<ArrayList<AlternativeMessage>>> outqueue, String mailboxName) {
        boolean success = false;


        for (Map.Entry<Integer, ArrayList<ArrayList<AlternativeMessage>>> entry : outqueue.entrySet()) {
            boolean foundMailbox = false;
            boolean subfail = false;
            boolean fail = false;

            LanguageInterpreter thisThread = threads.get(entry.getKey());

            for (int mb = 0; mb < main.mailboxes; mb++) {
                int inbox = (mb + startMailbox) % main.mailboxes;

                ArrayList<ArrayList<AlternativeMessage>> messages = entry.getValue();
                if (messages.size() == 0) {
                    break;
                }

                int fallbackMode = -1;
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

    public int tryReceiveInbox(LanguageInterpreter main, Run run, int inbox, int depth, String mailboxName) {
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

    public boolean sendTransaction(LanguageInterpreter main, Run run, int depth, String mailboxName) {
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

    public Integer transaction(LanguageInterpreter main, Run run, int depth, boolean send, String mailboxName) {
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
