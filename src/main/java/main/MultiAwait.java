package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiAwait extends Thread {
    private final List<List<TaskAction>> changesForThisThread;
    private final List<TaskAction> changesForForkedThread;
    private int currentTask;
    private int id;
    private ReentrantReadWriteLock lock;
    private DoublyLinkedList data;
    private ArrayList<MultiAwait> threads;
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
    private volatile boolean writeReset;
    private boolean readingStopped;

    private List<TaskState> availableTasks = new ArrayList<>();
    private Map<Integer, LoopEnvironment> callers;
    private Map<String, Integer> variables;
    private Map<Integer, Integer> returnValues;
    private boolean readingCancelled;
    private List<TaskAction> submissions;
    private Map<Integer, List<String>> loops;
    private List<Integer> handles;
    private Map<Integer, Integer> state;
    private List<String> taskState;
    private List<String> mainTask;
    private List<List<TaskAction>> callbacks;
    List<String> task2;
    List<String> task3;
    private int n = 0;
    private String currentState;

    public MultiAwait(int id,
                      DoublyLinkedList data) {
        this.id = id;
        this.data = data;
        loops = new HashMap<>();
        handles = new ArrayList<>();
        state = new HashMap<>();
        taskState = new ArrayList<>();
        mainTask = new ArrayList<>();
        task2 = new ArrayList<>();
        task3 = new ArrayList<>();
        submissions = new ArrayList<>();
        callbacks = new ArrayList<List<TaskAction>>();
        changesForThisThread = new ArrayList<>();
        changesForForkedThread = new ArrayList<>();
        variables = new HashMap<>();
        lock = new ReentrantReadWriteLock();
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 12;
        DoublyLinkedList data = new DoublyLinkedList(0, System.currentTimeMillis());
        List<MultiAwait> threads = new ArrayList<>();
        data.insert(1);


        for (int i = 0; i < threadCount; i++) {
            MultiAwait thread = new MultiAwait(i, data);
            thread.reading = true;
            thread.finishedReading = false;
            thread.readingCancelled = false;
            thread.clear = false;
            threads.add(thread);
        }


        for (int i = 0; i < threadCount; i++) {
            if (i % 2 == 1) {
                System.out.println(i);
                threads.get(i).writeReset = true;
            }
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
        int incremented = 0;
        for (int x = 0; x < threads.size(); x++) {
            if (threads.get(x).variables.get("value") == null) {
            }
            if (threads.get(x).variables.containsKey("value")) {
                incremented += threads.get(x).variables.get("value");
                System.out.println(String.format("Thread %d: %d", x,
                        threads.get(x).variables.get("value")));
            } else {
                System.out.println(String.format("Thread %d doesn't have a value", x));
                System.out.println(threads.get(x).state);
                System.out.println(threads.get(x).loops);
                System.out.println(threads.get(x).currentState);
            }
            if (threads.get(x).variables.get("value") < 1000) {
                System.out.println(String.format("Thread %d is low", x));
                System.out.println(threads.get(x).state);
                System.out.println(threads.get(x).loops);
                System.out.println(threads.get(x).currentState);
            }
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
        System.out.println(String.format("Requests per second %d", incremented));
    }

    public class TaskAction {

        public MultiAwait thread;
        public boolean handled;

        public TaskAction(MultiAwait thread) {
            this.thread = thread;
        }
    }


    public class Fork extends TaskAction {

        public List<String> taskState;
        private Map<Integer, List<String>> loops;
        private Map<Integer, Integer> state;
        private Map<String, Integer> variables;
        private List<List<TaskAction>> callbacks;
        private List<StateChange> stateChanges;
        private String task;
        private MultiAwait thread;
        private int awaiterTask;
        private int taskNo;
        private int loop;
        private String next;
        private LoopEnvironment loopEnvironment;
        private String currentState;

        public Fork(List<StateChange> stateChanges,
                    String task,
                    MultiAwait thread,
                    int awaiterTask,
                    int taskNo,
                    int loop,
                    String next,
                    LoopEnvironment loopEnvironment, String currentState,
                    List<String> taskState,
                    Map<Integer, List<String>> loops,
                    Map<Integer, Integer> state,
                    Map<String, Integer> variables, List<List<TaskAction>> callbacks) {
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
            this.taskState = taskState;
            this.loops = loops;
            this.state = state;
            this.variables = variables;
            this.callbacks = callbacks;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FORK ");
            sb.append(task);
            sb.append("\n");
            return sb.toString();
        }
    }

    private void setThreads(ArrayList<MultiAwait> tokenRingTimers) {
        this.threads = tokenRingTimers;
    }

    public void run() {
        int lastValue = 0;
        int next = 0;
        int previous = 0;

        if (id == threads.size() - 1) {
            previous = 0;
            next = 0;
        } else if (id == 0) {
          previous = threads.size() - 1;
          next = threads.size() - 1;
        } else {
            next = id + 1;
            previous = id + 1;
        }
        int strictNextThreadId = id + 1;
        if (id == threads.size() - 1) {
            strictNextThreadId = 0;
        }
        List<List<TaskAction>> previousCallbacks = threads.get(previous % threads.size()).callbacks;
        List<List<TaskAction>> nextCallbacks = threads.get(next % threads.size()).callbacks;
        callers = new HashMap<>();

        returnValues = new HashMap<>();

        mainTask.add("task1.print(\"task1. Starting main task\");");
        mainTask.add("task1.while True:");
        // task,loop

        ArrayList<String> innerloops = new ArrayList<>();
        innerloops.add("task1.handle1 = task2.fork();");
        loops.put(0, innerloops);


        task2.add("task2.print(\"task2. Starting task2 task\");");
        innerloops = new ArrayList<>();

        innerloops.add("task2.yieldwait");
        loops.put(1, innerloops);

        innerloops = new ArrayList<>();
        innerloops.add("task3.yieldwait");
        loops.put(2, innerloops);
        task3.add("task3.print(\"task3. Starting task3 task\");");

        task2.add("task2.while True:");
        task2.add("task2.value++");
        task2.add("task2.yield value");

        // initial task states
        taskState.add(mainTask.get(0));
        taskState.add(task2.get(0));
        taskState.add(task3.get(0));
        for (int i = 0 ; i < 3 ; i++) {
            callbacks.add(new ArrayList<>());
        }
        state.put(0, 0);
        state.put(1, 1);
        state.put(2, 1);

        currentTask = 0;
        while (running) {
            MultiAwait nextThread = threads.get((next) % threads.size());
            MultiAwait strictNextThread = threads.get(strictNextThreadId);
            MultiAwait previousThread = threads.get((previous) % threads.size());


//            System.out.println(String.format("Write thread for thread %d is %d", id, writeThread));
//            System.out.println(String.format("next thread for %d is %d", id, next));
//            System.out.println(String.format("previous thread for %d is %d", id, previous));
//            System.out.println(String.format("previous thread for %d is %d", id, strictNextThreadId));

            currentState = taskState.get(currentTask);
            if (reading) {
                readingCount++;
//                System.out.println(String.format("%d currentTask %d ==========================", id, currentTask));
                if (changesForThisThread.size() > 0) {
                    for (TaskAction await : changesForThisThread.get(0)) {
                        if (await.handled) {
                            continue;
                        }

                        if (await.getClass() == Fork.class) {
                            await.handled = true;
                            Fork fork = (Fork) await;
                            fork.thread.applyState(fork.stateChanges);
                            fork.thread.loops.get(fork.taskNo).set(fork.loop, fork.next);

//                         System.out.println(String.format("%d RUNNING FORK %s", id, fork.task));
                            // state.put(fork.taskNo, 0);
                            List<TaskAction> readingpre = runTask(fork.thread, previousThread, "readingpre", fork.awaiterTask,
                                    fork.thread.state,
                                    fork.thread.taskState,
                                    fork.thread.loops,
                                    fork.task,
                                    fork.thread.variables,
                                    fork.thread.callbacks,
                                    fork.thread.callbacks

                            );
                            handle(readingpre);

                        } else if (await.getClass() == StateChange.class) {
                            await.handled = true;
                            StateChange stateChange = (StateChange) await;
                            stateChange.thread.state.put(stateChange.task, stateChange.newState);
                        }
                    }

                    changesForThisThread.remove(0);
                }
//                System.out.println(changesForForkedThread.size());

                List<TaskAction> main = runTask(this, previousThread, "main",
                        currentTask,
                        state,
                        taskState,
                        loops,
                        currentState,
                        variables,
                        callbacks,
                        callbacks
                );
//                System.out.println(String.format("%d %s", id, main));
//                System.out.println(String.format("%d: %s", id, state));

                for (TaskAction await : main) {

//                    System.out.println(String.format("Handling write, %s", await));

                    if (await.getClass() == Yield.class) {
                        await.handled = true;
                        Yield yielded = (Yield) await;
//                        System.out.println(String.format("%d %s", id, yielded));
//                        System.out.println("ENCOUNTERED YIELD");
                        if (!yielded.doNothing) {
                            yielded.fork.thread.loops.get(yielded.awaitingTask)
                                    .set(yielded.loop, yielded.instruction);
                        }
                        yielded.fork.thread.applyState(yielded);
                        yielded.fork.thread.taskState.set(yielded.awaitingTask, yielded.currentState);


                    } else if (await.getClass() == Fork.class) {
                        Fork fork = (Fork) await;
                        fork.thread.loops.get(fork.taskNo).set(fork.loop, fork.next);
                        fork.thread.applyState(fork.stateChanges);

                        changesForForkedThread.add(fork);
                    } else if (await.getClass() == StateChange.class) {
                        StateChange stateChange = (StateChange) await;
                        stateChange.thread.state.put(stateChange.task, stateChange.newState);
                    }
                }

//                System.out.println(String.format("pending changes %s", pendingChanges));
                for (int i = 1; i < state.size(); i++) {

                    int task = (currentTask + i) % state.size();
                    if (state.get(task) == 0) {
                        currentTask = task;
                        break;
                    }
                }


                readingCancelled = false;
                readingCount++;
                data.read();
                lastValue = data.tail.value + id;
                finishedReading = true;


            }
            n++;



            if (n > 500 && writeReset) {
                n = 0;
//                System.out.println(String.format("%d writing", id));
                writingCount++;
//                System.out.println(String.format("%d Writing", id));
                data.insert(lastValue);
                clear = false;
                writeReset = false;


//                for (TaskAction await : changesForThisThread) {
//                    if (await.handled) { continue; }
////                    System.out.println(String.format("Handling write, %s", await));
//
//                    if (await.getClass() == StateChange.class) {
//                        StateChange stateChange = (StateChange) await;
//                        await.thread.state.put(stateChange.task, stateChange.newState);
//                    }
//                }



                    nextThread.changesForThisThread.add(new ArrayList<>(changesForForkedThread));
                    changesForForkedThread.clear();
                    threads.get(next).readingCancelled = false;
                    threads.get(next).finishedReading = false;
                    threads.get(next).reading = true;
                    readingCancelled = false;
                    reading = true;
                    finishedReading = false;
                    strictNextThread.writeReset = true;
                    // System.out.println(String.format("Passing the baton to %d", next));
//                    submissions.clear();



//                    System.out.println("0 turn to write");
            }
        }
    }

    private void handle(List<TaskAction> readingpreyield) {
        for (TaskAction action : readingpreyield) {
//            System.out.println(String.format("readingpre %s", action));
            if (action.getClass() == Yield.class) {
                System.out.println("encountered yield in next thread");
                Yield yielded = (Yield) action;
                yielded.fork.thread.applyState(yielded);
                yielded.fork.thread.loops.get(yielded.awaitingTask)
                        .set(yielded.loop, yielded.instruction);
                yielded.fork.thread.taskState.set(yielded.awaitingTask, yielded.currentState);
            }
            if (action.getClass() == Fork.class) {
                Fork fork = (Fork) action;
                fork.thread.loops.get(fork.taskNo).set(fork.loop, fork.next);
                fork.thread.applyState(fork.stateChanges);

            }

            if (action.getClass() == StateChange.class) {
                action.handled = true;
                StateChange stateChange = (StateChange) action;
                stateChange.thread.state.put(stateChange.task, stateChange.newState);
            }
        }
    }

    private void applyState(List<StateChange> stateChanges) {
        for (StateChange change : stateChanges) {
            change.thread.state.put(change.task, change.newState);
        }
    }

    private void applyState(Yield yielded) {
        for (StateChange change : yielded.stateChanges) {
            yielded.fork.thread.state.put(change.task, change.newState);
        }
    }

    private List<TaskAction> runTask(MultiAwait thread, MultiAwait nextThread, String caller, int awaitingTask,
                                     Map<Integer, Integer> state,
                                     List<String> taskState,
                                     Map<Integer, List<String>> loops,
                                     String currentState,
                                     Map<String, Integer> variables,
                                     List<List<TaskAction>> callbacks,
                                     List<List<TaskAction>> myCallbacks) {
        List<TaskAction> submissions = new ArrayList<>();
        List<Yield> callbacksToAdd = new ArrayList<>();


//        System.out.println(String.format("Current state: %s %s %s %s %s",
//                thread.state, currentState,
//                thread.loops.get(0).get(0),
//                thread.loops.get(1).get(0),
//                thread.loops.get(2).get(0)));

        switch (currentState) {
            case "task3.print(\"task3. Starting task3 task\");":
                System.out.println(
                        String.format("task3. Starting task3 task %s",
                                caller));
                thread.taskState.set(thread.currentTask, "task1.while True:");
                thread.taskState.set(2, "task3.while True:");
                thread.state.put(2, 0);
                break;
            case "task1.print(\"task1. Starting main task\");":
                System.out.println(
                        String.format("task1. Starting main task %s",
                                caller));
                thread.taskState.set(thread.currentTask, "task1.while True:");
                thread.state.put(0, 0);
                break;
            case "task1.while True:":
//                System.out.println(String.format("Running task %d %s %s task1 loop %s %s", id, currentState, caller, loops.get(0).get(0), caller));

                switch (thread.loops.get(0).get(0)) {
                    case "task1.handle1 = task2.fork();":
//                        System.out.println("task1.handle1 = task2.fork(), forking");
//                        state.put(1, 0); // resume task2
//                        System.out.println(state);
                        thread.loops.get(0).set(0, "task1.handle2 = task3.fork();");
                        thread.state.put(0, 0);
//                        loops.get(0).set(0, "task1.value = handle1.await()");

                        List<StateChange> stateChanges = new ArrayList<>();
                        stateChanges.add(new StateChange(thread, 1, 0));
                        stateChanges.add(new StateChange(thread, 0, 0));

//                        System.out.println(String.format("next is %s", loops.get(0).get(0)));
                        // awaitingTask.add(0);
                        // loops.get(1).set(0, "task2.yield value2++");
                        Fork fork2 = new Fork(stateChanges,
                                "task2.while True:",
                                thread,
                                0,
                                1,
                                0,
                                "task2.yield value2++",

                                new LoopEnvironment(0,
                                        0,
                                        "awaited.join", true),
                                "task1.while True:",
                                thread.taskState,
                                thread.loops,
                                thread.state,
                                thread.variables,
                                callbacks);
                        thread.lock.writeLock().lock();
                        callbacks.get(0).add(fork2);
                        thread.lock.writeLock().unlock();
                        submissions.add(fork2);
//                        System.out.println(String.format("Setting callbacks for thread %d %s", id, caller));
                        break;
                    case "task1.handle2 = task3.fork();":
//                        System.out.println("task1.handle2 = task3.fork(), forking");
//                        state.put(1, 0); // resume task2
//                        System.out.println(state);
                        thread.taskState.set(currentTask, "task3.print(\"task3. Starting task3 task\");");
                        thread.loops.get(0).set(0, "task1.value = handle2.await()");
                        //
                        thread.state.put(0, 0);
                        List<StateChange> stateChanges2 = new ArrayList<>();
                        stateChanges2.add(new StateChange(thread, 0, 0));
                        stateChanges2.add(new StateChange(thread, 2, 0));

//                        System.out.println(String.format("next is %s", loops.get(0).get(0)));
                        // awaitingTask.add(0);
                        // loops.get(1).set(0, "task2.yield value2++");
                        Fork fork3 = new Fork(stateChanges2,
                                "task3.while True:",
                                thread,
                                0,
                                2,
                                0,
                                "task3.yield value3++",

                                new LoopEnvironment(0,
                                        0,
                                        "awaited.join", false),
                                "task1.while True:",
                                thread.taskState,
                                thread.loops,
                                thread.state,
                                thread.variables,
                                callbacks);

                        thread.lock.writeLock().lock();
                        callbacks.get(0).add(fork3);
                        thread.lock.writeLock().unlock();
                        submissions.add(fork3);
//                        System.out.println(String.format("Setting callbacks for thread %d %s", id, caller));
                        break;
                    case "task1.value = handle1.await()":
//                        System.out.println(String.format("Awaiting %s", caller));
                        thread.state.put(0, 1); // pause
                        break;
                    case "task1.value = handle2.await()":
//                        System.out.println(String.format("Awaiting %s", caller));
                        thread.state.put(0, 1); // pause
                        break;
                    case "handle1.awaited":
//                        System.out.println(String.format("Await finished %s", caller));

                        break;
                    case "wait":
                        thread.state.put(0, 1);
                        break;

                    case "awaited.join":

//                        System.out.println(String.format("%d Awaited join %d %d",
//                                id,
//                                thread.callbacks.get(1).size(),
//                                thread.callbacks.get(2).size()));

                        List<TaskAction> taskActions = thread.callbacks.get(1);
                        List<TaskAction> removals = new ArrayList<>();
                        thread.lock.writeLock().lock();
                        for (TaskAction action : taskActions) {
                            Yield yielded = (Yield) action;
                            if (yielded.taskNo == 1) {
                                removals.add(action);
                                yielded.fork.variables.put("value", yielded.returnValue);
                                // yielded.fork.thread.loops.get(0).set(0, "awaited.join");
                                submissions.add(new StateChange(yielded.fork.thread, 0, 0));
//                                System.out.println("handled yield task1");
                            }
                        }
                        for (TaskAction removal : removals) {
                            taskActions.remove(removal);
                        }

//                        System.out.println(String.format("%d %d", callbacks.get(2).size(), callbacks.get(1).size()));


                        taskActions = thread.callbacks.get(2);
                        removals = new ArrayList<>();

                        for (TaskAction action : taskActions) {
                            Yield yielded = (Yield) action;
                            if (yielded.taskNo == 2) {
                                removals.add(action);
                                yielded.fork.variables.put("value4", yielded.returnValue);
                                // yielded.fork.thread.loops.get(0).set(0, "awaited.join");
                                submissions.add(new StateChange(yielded.fork.thread, 0, 0));
//                                System.out.println("handled yield task2");

                            }
                        }
                        for (TaskAction removal : removals) {
                            taskActions.remove(removal);
                        }
                        thread.lock.writeLock().unlock();

                        int sizeA = thread.callbacks.get(2).size();
                        int sizeB = thread.callbacks.get(1).size();
                        if (sizeA == 0 && sizeB == 0) {
//                                System.out.println("set to print");
                                thread.loops.get(0).set(0, "task1.print(value)");
                        }



                        break;
                    case "task1.print(value)":
                        System.out.println(String.format("%d Value: %s Value %s %s",
                                id, thread.variables.get("value"), thread.variables.get("value4"), caller));
                        thread.loops.get(0).set(0, "task1.handle1 = task2.fork();");

                        break;
                }
                break;
            case "task2.print(\"task2. Starting task2 task\");":
                System.out.println(String.format("task2. Starting task2 task %s",
                        caller));
                thread.taskState.add(currentTask, "task2.while True:");

                break;
            case "task2.while True:":
//                if (id == 1) {
//
                    // System.out.println(String.format("task2 loop %s %s", loops.get(1).get(0), caller));
//                }
                switch (thread.loops.get(1).get(0)) {
                    case "task2.yieldwait":
                        submissions.add(new StateChange(thread, 1, 1));
                        thread.state.put(1, 1);
                        break;
                    case "task2.yield value2++":
//                        System.out.println(String.format("%d %s %s", id, caller, callbacks));
//                        System.out.println("task2 yield");
                        List<TaskAction> removals = new ArrayList<>();
                        thread.lock.writeLock().lock();

                        for (TaskAction action : thread.callbacks.get(0)) {
                            Fork fork2 = (Fork) action;
                            if (fork2.taskNo == 1) {

                                removals.add(fork2);
                                //                        System.out.println(callbacks);
                                fork2.thread.state.put(1, 1);
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
                                 stateChanges.add(new StateChange(thread, 1, 1));
                                stateChanges.add(new StateChange(thread, fork2.awaiterTask, 0));
//                        System.out.println(String.format("IN YIELD %s %s", caller,
//                                fork2.task));
                                Yield yield = new Yield(fork2.thread, fork2,
                                        fork2.currentState,
                                        value2,
                                        stateChanges,
                                        fork2.awaiterTask,
                                        fork2.loopEnvironment.loop,
                                        fork2.loopEnvironment.instruction,
                                        1,
                                        fork2.loopEnvironment.doNothing);
                                submissions.add(yield);
                                callbacksToAdd.add(yield);
                            }
                        }
                        for (Yield yield : callbacksToAdd) {
                            yield.thread.callbacks.get(1).add(yield);
//                            System.out.println("Added callback1");
                        }
                        for (TaskAction removal : removals) {
                            thread.callbacks.get(0).remove(removal);
                        }
                        thread.lock.writeLock().unlock();

                        break;
                }
                break;
            case "task3.while True:":
//                System.out.println(String.format("Running task %d %s %s task3 loop %s %s", id, currentState, caller, loops.get(2).get(0), caller));

                switch (thread.loops.get(2).get(0)) {
                    case "task3.yieldwait":
                        submissions.add(new StateChange(thread, 2, 1));
                        thread.state.put(1, 1);
                        break;
                    case "task3.yield value3++":
//                        System.out.println("task3 yield");

//                            System.out.println(String.format("%d Yield2 %s ", id, caller));
//                            System.out.println(String.format("%d %s %s", id, caller, callbacks));
                        List<TaskAction> removals = new ArrayList<>();
                        thread.lock.writeLock().lock();
                        for (TaskAction action : thread.callbacks.get(0)) {
                            Fork fork3 = (Fork) action;
                            if (fork3.taskNo == 2) {
                                removals.add(fork3);
                                //                        System.out.println(callbacks);
                                fork3.thread.state.put(2, 1);
                                Integer value3 = 0;
                                if (fork3.thread.variables.containsKey("value3")) {
                                    value3 = fork3.thread.variables.get("value3");
                                }
                                value3++;
                                fork3.thread.variables.put("value3", value3);
    //                        returnValues.put(1, value2);
                                // resume caller
    //                        Integer remove = awaitingTask;
                                // loops.get(remove).set(callers.get(1).loop, callers.get(1).instruction);
    //                        state.put(remove, 0);
    //                        loops.get(1).set(0, "task2.yieldwait");
    //                        state.put(1, 1);
                                List<StateChange> stateChanges = new ArrayList<>();
                                stateChanges.add(new StateChange(thread, 2, 1));
                                stateChanges.add(new StateChange(thread, fork3.awaiterTask, 0));
    //                        System.out.println(String.format("IN YIELD %s %s", caller,
    //                                fork3.task));
                                Yield yield = new Yield(fork3.thread, fork3,
                                        fork3.currentState,
                                        value3,
                                        stateChanges,
                                        fork3.awaiterTask,
                                        fork3.loopEnvironment.loop,
                                        fork3.loopEnvironment.instruction,
                                        2,
                                        fork3.loopEnvironment.doNothing);
                                submissions.add(yield);
                                callbacksToAdd.add(yield);
                            }
                        }
                        for (TaskAction removal : removals) {
                            thread.callbacks.get(0).remove(removal);
                        }
                        for (Yield yield : callbacksToAdd) {
                            yield.thread.callbacks.get(2).add(yield);
//                            System.out.println("Added callback2");
                        }
                        thread.lock.writeLock().unlock();
                        break;



                }
            break;
        }
        return submissions;
    }

    private Map<Integer, List<String>> clone(Map<Integer, List<String>> loops) {
        HashMap<Integer, List<String>> hm = new HashMap<>();
        for (Map.Entry<Integer, List<String>> entry : loops.entrySet()) {
            hm.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return hm;
    }

    private class LoopEnvironment {
        private final int task;
        private final int loop;
        private final String instruction;
        public boolean doNothing;

        public LoopEnvironment(int task, int loop, String instruction, boolean doNothing) {
            this.task = task;
            this.loop = loop;
            this.instruction = instruction;
            this.doNothing = doNothing;
        }
    }

    private class StateChange extends TaskAction {
        private final int task;
        private final int newState;

        public StateChange(MultiAwait thread, int task, int newState) {
            super(thread);
            this.task = task;
            this.newState = newState;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.task);
            sb.append("->");
            sb.append(this.newState);
            return sb.toString();
        }
    }

    private class Yield extends TaskAction {
        private final List<StateChange> stateChanges;
        private final int awaitingTask;
        private final int loop;
        private final String instruction;
        private final boolean doNothing;
        public int taskNo;
        private Fork fork;
        private String currentState;
        public Integer returnValue;

        public Yield(MultiAwait thread,
                     Fork fork,
                     String currentState,
                     Integer returnValue,
                     List<StateChange> stateChanges,
                     int awaitingTask,
                     int loop,
                     String instruction,
                     int taskNo,
                     boolean doNothing) {
            super(thread);
            this.taskNo = taskNo;
            this.thread = thread;
            this.fork = fork;
            this.currentState = currentState;
            this.returnValue = returnValue;

            this.stateChanges = stateChanges;
            this.awaitingTask = awaitingTask;
            this.loop = loop;
            this.instruction = instruction;
            this.doNothing = doNothing;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("YIELD:");
            sb.append(currentState);
            sb.append(" ");
            sb.append(returnValue);
            sb.append(" ");
            sb.append(stateChanges);
            sb.append(" ");
            sb.append(awaitingTask);
            sb.append(" ");
            sb.append(loop);
            sb.append(" ");
            sb.append(instruction);
            sb.append(" ");
            sb.append("ENDYIELD");
            return sb.toString();
        }
    }
}
