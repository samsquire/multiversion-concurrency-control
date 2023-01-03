package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenRingTimer2AsyncAwait extends Thread {
    private int currentTask;
    private int id;
    private DoublyLinkedList data;
    private ArrayList<TokenRingTimer2AsyncAwait> threads;
    private boolean running = true;
    private volatile boolean clear = false;
    private volatile boolean wakeup = false;
    private volatile boolean acknowledge = false;
    private volatile boolean finishedReading = false;
    private volatile boolean reading = false;
    private int writingCount;
    private int readingCount;
    private boolean finishedWrite;
    private boolean allFinished = true;
    private boolean writeReset;
    private boolean readingStopped;

    private List<TaskState> availableTasks = new ArrayList<>();
    private Map<Integer, LoopEnvironment> callers;
    private Map<String, Integer> variables;
    private Map<Integer, Integer> returnValues;
    private boolean readingCancelled;

    public TokenRingTimer2AsyncAwait(int id,
                                     DoublyLinkedList data) {
        this.id = id;
        this.data = data;
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 11;
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        List<TokenRingTimer2AsyncAwait> threads = new ArrayList<>();
        data.insert(1);


        for (int i = 0; i < threadCount; i++) {
            TokenRingTimer2AsyncAwait thread = new TokenRingTimer2AsyncAwait(i, data);
            thread.reading = true;
            thread.finishedReading = true;
            thread.allFinished = true;
            threads.add(thread);
        }
        threads.get(0).writeReset = true;
        for (int i = 0; i < threadCount; i++) {
            threads.get(i).setThreads(new ArrayList<>(threads));
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

        List<Integer> items = new ArrayList<>();
        DoublyLinkedList current = data;
        while (current != null) {
            items.add(current.value);
            current = current.tail;
        }
        int readings = 0;
        int writings = 0;
        for (int x = 0; x < threads.size(); x++) {
            readings += threads.get(x).readingCount;
            writings += threads.get(x).writingCount;
        }

        double seconds = (end - start) / 1000.0;
//        System.out.println(String.format("Total Requests %d", items.size()));
//        System.out.println(String.format("Time elapsed %f", seconds));
//        System.out.println(String.format("Readings %d", readings));
//        System.out.println(String.format("Writings %d", writings));
        int sumRW = readings + writings;
//        System.out.println(String.format("Total reading+writings %d", sumRW));
        System.out.println(String.format("Total reading+writings per second %f", sumRW / seconds));
        System.out.println(String.format("Requests per second %f",
                items.size() / seconds));
    }

    private void setThreads(ArrayList<TokenRingTimer2AsyncAwait> tokenRingTimers) {
        this.threads = tokenRingTimers;
    }

    public void run() {
        int lastValue = 0;
        int next = id + 1;
        int last = threads.size() - 1;
        callers = new HashMap<>();
        variables = new HashMap<>();
        returnValues = new HashMap<>();
        List<Integer> handles = new ArrayList<>();
        Map<Integer, Integer> state = new HashMap<>();
        List<String> taskState = new ArrayList<>();
        List<String> mainTask = new ArrayList<>();
        List<String> task2 = new ArrayList<>();
        mainTask.add("task1.print(\"task1. Starting main task\");");
        mainTask.add("task1.while True:");
        // task,loop
        HashMap<Integer, ArrayList<String>> loops = new HashMap<>();

        ArrayList<String> innerloops = new ArrayList<>();
        innerloops.add("task1.handle0 = task2.fork();");
        loops.put(0, innerloops);


        task2.add("task2.print(\"task2. Starting task2 task\");");
        innerloops = new ArrayList<>();

        innerloops.add("task2.yieldwait");
        loops.put(1, innerloops);

        task2.add("task2.while True:");
        task2.add("task2.value++");
        task2.add("task2.yield value");

        // initial task states
        taskState.add(mainTask.get(0));
        taskState.add(task2.get(0));
        state.put(0, 0);
        state.put(1, 1);
        List<TaskState> forkings = new ArrayList<>();
        currentTask = 0;
        while (running) {
            readingCount++;
            String currentState = taskState.get(currentTask);
//            System.out.println(String.format("currentTask% d ==========================", currentTask));
            if (reading) {
                // System.out.println("Reading");
                lastValue = data.tail.value + id;
                finishedReading = true;

                switch (currentState) {
                    case "task1.print(\"task1. Starting main task\");":
                        System.out.println("task1. Starting main task");
                        taskState.set(currentTask, "task1.while True:");
                        state.put(0, 0);
                        break;
                    case "task1.while True:":
                        switch (loops.get(0).get(0)) {
                            case "task1.handle0 = task2.fork();":
//                            System.out.println("task1.handle0 = task2.fork(), forking");
                                state.put(1, 0); // resume task2
//                            System.out.println(state);
                                loops.get(0).set(0, "task1.value = handle.await()");
                                callers.put(1, new LoopEnvironment(0, 0, "handle.awaited"));

                                loops.get(1).set(0, "task2.yield value2++");
//                            System.out.println(String.format("next is %s", loops.get(0).get(0)));
                                handles.add(0);
                                break;
                            case "task1.value = handle.await()":
//                            System.out.println("Awaiting");
                                state.put(0, 1); // pause
                                System.out.println("put callers");
                                break;
                            case "handle.awaited":
//                            System.out.println("Await finished");
                                variables.put("value", returnValues.get(1));
                                loops.get(0).set(0, "task1.print(value)");
                                break;
                            case "task1.print(value)":
                                System.out.println(variables.get("value"));
                                loops.get(0).set(0, "task1.handle0 = task2.fork();");

                        }
                        break;
                    case "task2.print(\"task2. Starting task2 task\");":
                        System.out.println("task2. Starting task2 task");
                        taskState.add(currentTask, "task2.while True:");

                        break;
                    case "task2.while True:":
                        switch (loops.get(1).get(0)) {
                            case "task2.yieldwait":
                                state.put(1, 1);
                                break;
                            case "task2.yield value2++":
//                            System.out.println("yielding value");
//                            System.out.println("handles");
//                            System.out.println(handles);
                                Integer value2 = 0;
                                if (variables.containsKey("value2")) {
                                    value2 = variables.get("value2");
                                }
                                value2++;
                                variables.put("value2", value2);
                                returnValues.put(1, value2);
                                // resume caller
                                Integer remove = handles.remove(0);
                                loops.get(remove).set(callers.get(1).loop, callers.get(1).instruction);
                                state.put(remove, 0);
                                loops.get(1).set(0, "task2.yieldwait");
                                state.put(1, 1);

                        }
                        break;
                }
            } else {
                readingCancelled = true;
            }

            for (int i = -1; i < state.size(); i++) {

                int task = (currentTask + i + 1) % state.size();
                // System.out.println(String.format("%d is %d", task, state.get(task)));
                if (state.get(task) == 0) {
                    currentTask = task;
                }
            }


            if (id == 0 && writeReset) {
                if (finishedReading) {
                    boolean allFinished = true;
                    for (TokenRingTimer2AsyncAwait thread : threads) {
                        if (!thread.finishedReading) {
                            allFinished = false;
                            // System.out.println(String.format("%d not finished reading",
                            // thread.id));
                            break;
                        }
                    }
                    if (allFinished) {
                        // System.out.println(String.format("Finished reading %s", finishedReading));

                        // System.out.println("Stopping threads");
                        for (TokenRingTimer2AsyncAwait thread : threads) {
                            thread.reading = false;
                        }
                        boolean allCancelled = true;
                        for (TokenRingTimer2AsyncAwait thread : threads) {
                            if (thread != this) {
                                if (!thread.readingCancelled) {
                                    allCancelled = false;
                                    // System.out.println(String.format("%d is not cancelled", thread.id));
                                }
                            }
                        }
                        // System.out.println(String.format("State %s %s %s", allFinished, allCancelled, writeReset));
                        if (allFinished && allCancelled && writeReset) {
                            clear = true;
                            writeReset = false;
                            // System.out.println("New cycle");
                        }
                    }
                }
            }

            if (clear) {
                writingCount++;
                // System.out.println("Writing");
                data.insert(lastValue);
                // System.out.println(String.format("%d writing", id));
                clear = false;


                if (next == last) {
                    for (TokenRingTimer2AsyncAwait thread : threads) {
                        thread.finishedReading = false;
                        thread.reading = true;
                        thread.readingStopped = false;

                    }
                    threads.get(0).writeReset = true;
                } else {
                    TokenRingTimer2AsyncAwait nextThread = threads.get((next) % threads.size());
                    nextThread.clear = true;
                    // System.out.println("Passing the baton");

                }
            }
        }
    }

    class LoopEnvironment {
        private final int task;
        public final int loop;
        public final String instruction;

        public LoopEnvironment(int task, int loop, String instruction) {
            this.task = task;
            this.loop = loop;
            this.instruction = instruction;
        }
    }
}