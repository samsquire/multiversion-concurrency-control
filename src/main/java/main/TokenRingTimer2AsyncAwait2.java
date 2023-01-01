package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenRingTimer2AsyncAwait2 extends Thread {
    private final List<TaskAction> changesForThisThread;
    private final List<TaskAction> changesForForkedThread;
    private int currentTask;
    private int id;
    private DoublyLinkedList data;
    private ArrayList<TokenRingTimer2AsyncAwait2> threads;
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
    private List<TaskAction> submissions;
    private HashMap<Integer, ArrayList<String>> loops;
    private List<Integer> handles;
    private Map<Integer, Integer> state;
    private List<String> taskState;
    private List<String> mainTask;
    private TaskAction[] callbacks;
    List<String> task2;
    public TokenRingTimer2AsyncAwait2(int id,
                                      DoublyLinkedList data) {
        this.id = id;
        this.data = data;
        loops = new HashMap<>();
        handles = new ArrayList<>();
        state = new HashMap<>();
        taskState = new ArrayList<>();
        mainTask = new ArrayList<>();
        task2 = new ArrayList<>();
        submissions = new ArrayList<>();
        callbacks = new TaskAction[2];
        changesForThisThread = new ArrayList<>();
        changesForForkedThread = new ArrayList<>();

    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 2;
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        List<TokenRingTimer2AsyncAwait2> threads = new ArrayList<>();
        data.insert(1);


        for (int i = 0; i < threadCount; i++) {
            TokenRingTimer2AsyncAwait2 thread = new TokenRingTimer2AsyncAwait2(i, data);
            thread.reading = true;
            thread.finishedReading = false;
            thread.readingCancelled = false;
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

    public class TaskAction {

        public TokenRingTimer2AsyncAwait2 thread;
        public boolean handled;

        public TaskAction(TokenRingTimer2AsyncAwait2 thread) {
            this.thread = thread;
        }
    }



    public class Fork extends TaskAction {

        private List<StateChange> stateChanges;
        private String task;
        private TokenRingTimer2AsyncAwait2 thread;
        private final int awaiterTask;
        private final int taskNo;
        private final int loop;
        private final String next;
        private LoopEnvironment loopEnvironment;
        private String currentState;

        public Fork(List<StateChange> stateChanges,
                    int handles,
                    String task,
                    TokenRingTimer2AsyncAwait2 thread,
                    int awaiterTask,
                    int taskNo,
                    int loop,
                    String next,
                    LoopEnvironment loopEnvironment, String currentState) {
            super(thread);
            this.stateChanges = stateChanges;
            this.task = task;
            this.thread = thread;
            this.awaiterTask = awaiterTask;
            this.taskNo = taskNo;
            this.loop = loop;
            this.next = next;
            this.loopEnvironment = loopEnvironment;
            this.currentState = currentState;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FORK ");
            sb.append(task);
            sb.append("\n");
            return sb.toString();
        }
    }

    private void setThreads(ArrayList<TokenRingTimer2AsyncAwait2> tokenRingTimers) {
        this.threads = tokenRingTimers;
    }

    public void groupChanges(List<TaskAction> changes) {

    }

    public void run() {
        int lastValue = 0;
        int next = id + 1;
        int last = threads.size() - 1;
        callers = new HashMap<>();
        variables = new HashMap<>();
        returnValues = new HashMap<>();

        mainTask.add("task1.print(\"task1. Starting main task\");");
        mainTask.add("task1.while True:");
        // task,loop

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
        List<TaskAction> pendingChanges = new ArrayList<>();

        currentTask = 0;

        while (running) {

            String currentState = taskState.get(currentTask);
            if (reading) {
                readingCount++;
                System.out.println(String.format("%d currentTask %d ==========================", id, currentTask));
                for (TaskAction await : changesForThisThread) {
                    if (await.handled) { continue; }
//                    System.out.println(String.format("processing submission to thread %s %s", id, await));
                    if (await.getClass() == Yield.class) {
                        await.handled = true;
                        Yield yielded = (Yield) await;
                        System.out.println("Running last batch yield");
                        pendingChanges.addAll(runTask("readingpreyield", yielded.awaitingTask,
                                yielded.fork.thread.state,
                                yielded.fork.thread.taskState,
                                yielded.fork.thread.loops,
                                yielded.currentState));
                    } else if (await.getClass() == Fork.class) {
                        await.handled = true;
                        Fork fork = (Fork) await;
//                        System.out.println(String.format("RUNNING FORK %s", fork.task));
                        // state.put(fork.taskNo, 0);
                        loops.get(fork.taskNo).set(fork.loop, fork.next);

                        applyState(fork.stateChanges);
                        pendingChanges.addAll(runTask("readingpre", fork.awaiterTask,
                                state,
                                taskState,
                                loops,
                                fork.task));
//                        System.out.println(String.format("Read pending changes %s", pendingChanges));
                    }

                    else if (await.getClass() == StateChange.class) {
                        await.handled = true;
                        StateChange stateChange = (StateChange) await;
                        state.put(stateChange.task, stateChange.newState);
                    } else {
                        changesForThisThread.add(await);
                    }
                }
                changesForThisThread.clear();
                changesForThisThread.addAll(pendingChanges);
                pendingChanges.clear();
                changesForThisThread.addAll(runTask("main",
                        currentTask,
                        state,
                        taskState,
                        loops,
                        currentState
                ));
                for (TaskAction await : changesForThisThread) {

                    if (await.getClass() == StateChange.class) {
                        StateChange stateChange = (StateChange) await;
                        await.thread.state.put(stateChange.task, stateChange.newState);
                    } else {
                        changesForForkedThread.add(await);
                    }
                }
//                System.out.println(String.format("pending changes %s", pendingChanges));
                for (int i = -1; i < state.size(); i++) {

                    int task = (currentTask + i + 1) % state.size();
                // System.out.println(String.format("%d is %d", task, state.get(task)));
                if (state.get(task) == 0) {
                    currentTask = task;
                }
            }




                readingCancelled = false;
                readingCount++;
                data.read();
                lastValue = data.tail.value + id;
                finishedReading = true;



            } else {
                readingCancelled = true;
            }
            if (id == 0 && writeReset) {
                if (finishedReading) {
                    boolean allFinished = true;
                    for (TokenRingTimer2AsyncAwait2 thread : threads) {
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
                        for (TokenRingTimer2AsyncAwait2 thread : threads) {
                            thread.reading = false;
                        }
                        boolean allCancelled = true;
                        for (TokenRingTimer2AsyncAwait2 thread : threads) {
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
//                System.out.println(String.format("%d Writing", id));
                data.insert(lastValue);
                clear = false;

                for (TaskAction await : changesForThisThread) {
                    if (await.handled) { continue; }
//                    System.out.println(String.format("Handling write, %s", await));
                    if (await.getClass() == Yield.class) {
                        System.out.println("ENCOUNTERED YIELD");
                        Yield yielded = (Yield) await;
                        yielded.fork.thread.applyState(yielded);

                        yielded.fork.thread.loops.get(yielded.awaitingTask)
                                .set(yielded.loop, yielded.instruction);

                        System.out.println(String.format("Setting loop instruction to %s %d", yielded.instruction, yielded.awaitingTask));
                        System.out.println(yielded.fork.thread.loops);
                        System.out.println(yielded.fork.thread.state);
                        System.out.println(yielded.fork.thread.id);
//                        System.out.println(yielded);

//                        pendingChanges.addAll(runTask("writingloop", yielded.awaitingTask,
//                                yielded.fork.thread.state,
//                                yielded.fork.thread.taskState,
//                                yielded.fork.thread.loops,
//                                yielded.currentState));
                    }
                    if (await.getClass() == StateChange.class) {
                        StateChange stateChange = (StateChange) await;
                        await.thread.state.put(stateChange.task, stateChange.newState);
                    }
                }

                if (next == last) {
                    TokenRingTimer2AsyncAwait2 nextThread = threads.get((next) % threads.size());
                    nextThread.changesForThisThread.addAll(changesForForkedThread);
                    // System.out.println(String.format("Passing the baton to %d", next));
//                    submissions.clear();

//                    nextThread.clear = true;
                    for (TokenRingTimer2AsyncAwait2 thread : threads) {
                        thread.readingCancelled = false;
                        thread.finishedReading = false;
                        thread.reading = true;
                    }
                    threads.get(0).writeReset = true;
//                    System.out.println("0 turn to write");
                } else {
                    TokenRingTimer2AsyncAwait2 nextThread = threads.get((next) % threads.size());
                    nextThread.changesForThisThread.addAll(changesForForkedThread);
                    // System.out.println(String.format("Passing the baton to %d", next));
//                    submissions.clear();

                    nextThread.clear = true;
                }
            }
        }
    }

    private void applyState(List<StateChange> stateChanges) {
        for (StateChange change : stateChanges) {
            state.put(change.task, change.newState);
        }
    }

    private void applyState(Yield yielded) {
        for (StateChange change : yielded.stateChanges) {
            state.put(change.task, change.newState);
        }
    }

    private List<TaskAction> runTask(String caller, int awaitingTask,
                                     Map<Integer, Integer> state,
                                     List<String> taskState,
                                     HashMap<Integer, ArrayList<String>> loops,
                                     String currentState) {
        List<TaskAction> submissions = new ArrayList<>();
        System.out.println(String.format("Running task %d %s %s", id, currentState, caller));
        switch (currentState) {
            case "task1.print(\"task1. Starting main task\");":
                System.out.println(
                        String.format("task1. Starting main task %s",
                                caller));
                taskState.set(currentTask, "task1.while True:");
                state.put(0, 0);
                break;
            case "task1.while True:":
                System.out.println(String.format("task1 loop %s %s", loops.get(0).get(0), caller));
                switch (loops.get(0).get(0)) {
                    case "task1.handle0 = task2.fork();":
//                        System.out.println("task1.handle0 = task2.fork(), forking");
//                        state.put(1, 0); // resume task2
//                        System.out.println(state);
                        loops.get(0).set(0, "task1.value = handle.await()");
                        state.put(0, 1);
                        List<StateChange> stateChanges = new ArrayList<>();
                        stateChanges.add(new StateChange(this,1, 0));

//                        System.out.println(String.format("next is %s", loops.get(0).get(0)));
                        // awaitingTask.add(0);
                        // loops.get(1).set(0, "task2.yield value2++");
                        Fork fork2 = new Fork(stateChanges,
                                awaitingTask,
                                "task2.while True:",
                                this,
                                0,
                                1,
                                0,
                                "task2.yield value2++",

                                new LoopEnvironment(0,
                                        0,
                                        "handle.awaited"),
                                currentState);
                        callbacks[0] = fork2;
                        submissions.add(fork2);
                        break;
                    case "task1.value = handle.await()":
//                        System.out.println(String.format("Awaiting %s", caller));
                        state.put(0, 1); // pause
                        break;
                    case "handle.awaited":
                        System.out.println(String.format("Await finished %s", caller));
                        TaskAction taskAction = callbacks[1];
                        Yield yielded = (Yield) taskAction;
                        yielded.fork.thread.variables.put("value", yielded.returnValue);
                        yielded.fork.thread.loops.get(0).set(0, "task1.print(value)");
                        submissions.add(new StateChange(yielded.fork.thread, 0, 0));
                        break;
                    case "task1.print(value)":
                        System.out.println(String.format("%d Value: %s %s",
                                id, variables.get("value"),  caller));
                        loops.get(0).set(0, "task1.handle0 = task2.fork();");

                }
                break;
            case "task2.print(\"task2. Starting task2 task\");":
                System.out.println(String.format("task2. Starting task2 task %s",
                        caller));
                taskState.add(currentTask, "task2.while True:");

                break;
            case "task2.while True:":
                System.out.println(String.format("task2 loop %s %s", loops.get(1).get(0), caller));

                switch (loops.get(1).get(0)) {
                    case "task2.yieldwait":
                        submissions.add(new StateChange(this, 1, 1));
                        state.put(1, 1);
                        break;
                    case "task2.yield value2++":
                        System.out.println(String.format("%d Yield %s ", id, caller));
                        state.put(1, 1);
                        Fork fork2 = (Fork) callbacks[0];
                        Integer value2 = 0;
                        if (fork2.thread.variables.containsKey("value2")) {
                            value2 = fork2.thread.variables.get("value2");
                        }
                        value2++;
                        fork2.thread.variables.put("value2", value2);
//                        returnValues.put(1, value2);
                        // resume caller
//                        Integer remove = awaitingTask;
                        // loops.get(remove).set(callers.get(1).loop, callers.get(1).instruction);
//                        state.put(remove, 0);
//                        loops.get(1).set(0, "task2.yieldwait");
//                        state.put(1, 1);
                        List<StateChange> stateChanges = new ArrayList<>();
                        stateChanges.add(new StateChange(this,1, 0));
                        stateChanges.add(new StateChange(this, awaitingTask, 0));
//                        System.out.println(String.format("IN YIELD %s %s", caller,
//                                fork2.task));
                        Yield yield = new Yield(fork2.thread, fork2,
                                fork2.currentState,
                                value2,
                                stateChanges,
                                fork2.awaiterTask,
                                fork2.loopEnvironment.loop,
                                fork2.loopEnvironment.instruction);
                        submissions.add(yield);
                        callbacks[1] = yield;

                }
                break;
        }
        return submissions;
    }

    private class LoopEnvironment {
        private final int task;
        private final int loop;
        private final String instruction;

        public LoopEnvironment(int task, int loop, String instruction) {
            this.task = task;
            this.loop = loop;
            this.instruction = instruction;
        }
    }

    private class StateChange extends TaskAction {
        private final int task;
        private final int newState;

        public StateChange(TokenRingTimer2AsyncAwait2 thread, int task, int newState) {
            super(thread);
            this.task = task;
            this.newState = newState;
        }
    }

    private class Yield extends TaskAction {
        private final List<StateChange> stateChanges;
        private final int awaitingTask;
        private final int loop;
        private final String instruction;
        private Fork fork;
        private String currentState;
        public Integer returnValue;

        public Yield(TokenRingTimer2AsyncAwait2 thread,
                     Fork fork,
                     String currentState,
                     Integer returnValue,
                     List<StateChange> stateChanges,
                     int awaitingTask,
                     int loop,
                     String instruction) {
            super(thread);
            this.thread = thread;
            this.fork = fork;
            this.currentState = currentState;
            this.returnValue = returnValue;

            this.stateChanges = stateChanges;
            this.awaitingTask = awaitingTask;
            this.loop = loop;
            this.instruction = instruction;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("YIELD:");
            sb.append(currentState);
            sb.append("\n");
            sb.append(returnValue);
            sb.append("\n");
            sb.append(stateChanges);
            sb.append("\n");
            sb.append(awaitingTask);
            sb.append("\n");
            sb.append(loop);
            sb.append("\n");
            sb.append(instruction);
            sb.append("\n");
            sb.append("ENDYIELD");
            return sb.toString();
        }
    }
}
