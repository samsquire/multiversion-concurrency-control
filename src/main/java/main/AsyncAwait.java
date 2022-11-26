package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsyncAwait {
    public static void main(String arg[]) {
        // 0 means ready
        HashMap<Integer, Integer> state = new HashMap<>();
        state.put(0, 0);
        state.put(1, 1);
        state.put(2, 1);
        HashMap<Integer, Integer> yielded = new HashMap<>();
        state.put(0, 0);
        state.put(1, 0);
        state.put(2, 0);
        HashMap<Integer, Integer> waitingFor = new HashMap<>();
        HashMap<Integer, Integer> returnvalue = new HashMap<>();
        List<String> taskState = new ArrayList<>();
        taskState.add("task1.print(\"Starting task1\")"); // task1
        taskState.add("task2.print(\"Starting task2\")"); // task2
        taskState.add("task3.print(\"Starting task3\")"); // task3
        HashMap<Integer, ArrayList<String>> loops = new HashMap<>();
        HashMap<Integer, Map<Integer, String>> loopreturn = new HashMap<>();
        ArrayList<String> innerloops = new ArrayList<>();
        innerloops.add("print(\"Hi\")");
        loops.put(0, innerloops);
        loopreturn.put(0, new HashMap<>());

        innerloops = new ArrayList<>();
        innerloops.add("task2.preyield1");
        loops.put(1, innerloops);
        loopreturn.put(1, new HashMap<>());

        innerloops = new ArrayList<>();
        innerloops.add("task3.preyield1");
        loops.put(2, innerloops);
        loopreturn.put(2, new HashMap<>());

        HashMap<Integer, String> returnstatement = new HashMap<>();
        HashMap<String, Integer> variables = new HashMap<>();
        int currenttask = 0;
        int currentLoop = 0;
        HashMap<Integer, Integer> loopYielding = new HashMap<>();
        variables.put("x", 10);
        variables.put("y", 0);
        variables.put("b", 10000);
        variables.put("n", 20000);


        while (true) { // vertical
            switch (taskState.get(currenttask)) {
                case "task1.print(\"Starting task1\")":
                    System.out.println("Starting task1");
                    taskState.set(currenttask, "task1.while True");

                    break;
                case "task2.print(\"Starting task2\")":
                    System.out.println("Starting task2");
                    taskState.set(currenttask, "task2.while True");
                    state.put(1, 0);
                    break;
                case "task3.print(\"Starting task3\")":
                    System.out.println("Starting task3");
                    taskState.set(currenttask, "task3.while True");
                    state.put(2, 0);
                    break;


                case "task1.while True":
//
                    if (loops.get(0).size() == 0) {
                        ArrayList<String> loopStart = new ArrayList<>();
                        loops.get(0).set(0, "task1.value1 = await task2()");
                    }
//                    System.out.println(String.format("Loop next %s", loops.get(0).get(0)));
                    switch (loops.get(0).get(0)) { // horizontal
                        case "print(\"Hi\")":
                            state.put(1, 0);
                            loops.get(0).set(0, "task1.value1 = await task2()");
                            break;
                        case "task1.value3 = await task2()":
                            state.put(1, 0); // wake up task2
                            waitingFor.put(0, 1);
                            returnstatement.put(0, "task1.while True");
                            loopreturn.get(0).put(0, "task1.value3");
                            loopYielding.put(0, 0);

                            loops.get(1).set(0, "task2.yield b++");
                            state.put(0, 1);
                            break;
                        case "task1.value3":
                            Integer value = returnvalue.get(1);
//                            System.out.println(String.format("Setting value1 to %s", value));
                            variables.put("value3", value);
                            state.put(0, 0);
                            loops.get(0).set(0, "task1.value4 = await task3()");
                            returnstatement.put(0, "task1.while True");
                            // loopreturn.get(0).put(0, "task1.value2 = await task3()");

                            break;
                        case "task1.value4 = await task3()":

                            state.put(2, 0); // wake up task3
                            waitingFor.put(0, 2);
                            returnstatement.put(0, "task1.while True");
                            loopreturn.get(0).put(0, "task1.value4");
                            loopYielding.put(0, 0);

                            loops.get(2).set(0, "task3.yield n++");
                            state.put(0, 1);
                            break;
                        case "task1.value4":
                            value = returnvalue.get(2);
//                            System.out.println(String.format("Setting value1 to %s", value));
                            variables.put("value4", value);
                            state.put(0, 0);
                            loops.get(0).set(0, "task1.value2 = await task3()");
                            loopreturn.get(0).put(0, "task1.value2 = await task3()");
                            returnstatement.put(0, "task1.while True");

                            break;
                        case "task1.value1 = await task2()":
                            state.put(1, 0); // wake up task2
                            waitingFor.put(0, 1);
                            returnstatement.put(0, "task1.while True");
                            loopreturn.get(0).put(0, "task1.value2");
                            loopYielding.put(0, 0);

                            loops.get(1).set(0, "task2.yield y++");
                            state.put(0, 1);
                            break;
                        case "task1.value1":
                            value = returnvalue.get(1);
//                            System.out.println(String.format("Setting value1 to %s", value));
                            variables.put("value1", value);
                            state.put(0, 0);
                            loops.get(0).set(0, "task1.value3 = await task2()");
                            returnstatement.put(0, "task1.while True");

                            break;
                        case "task1.value2":
//                            System.out.println(String.format("Setting value2 %s", returnvalue.get(2)));

                            variables.put("value2", returnvalue.get(2));
                            returnstatement.put(0, "task1.while True");
                            state.put(0, 0);
                            loops.get(0).set(0, "task1.print(\"Hello world\", value1, value2)");
                            break;
                        case "task1.value2 = await task3()":
                            state.put(2, 0); // wake up task3
                            waitingFor.put(0, 2);
                            returnstatement.put(0, "task1.while True");
                            state.put(0, 1);
                            loopreturn.get(0).put(0, "task1.value2");
                            loopYielding.put(0, 0);

                            loops.get(2).set(0, "task3.yield x++");

                            break;
                        case "task1.print(\"Hello world\", value1, value2)":
                            System.out.println(String.format("Hello World %d %d", variables.get("value1"), variables.get("value2")));
                            // returnstatement.put(0, "task1.while True");
                            loops.get(0).set(0, "task1.print(\"Hi\")");
                            loopreturn.get(0).put(0, "task1.print(\"Hi\")");
                            loopYielding.put(0, 0);


                            state.put(0, 0);
                            break;
                        case "task1.print(\"Hi\")":
                            System.out.println(String.format("Hi %d %d", variables.get("value3"), variables.get("value4")));
                            // returnstatement.put(0, "task1.while True");
                            loops.get(0).set(0, "task1.value1 = await task2()");
                            loopreturn.get(0).put(0, "task1.value1 = await task2()");
                            loopYielding.put(0, 0);


                            state.put(0, 0);
                            break;
                    }
                    break;


                case "task2.while True":
                    currentLoop = 1;
//                    System.out.println(loops.get(1).get(0));

                    if (loops.get(1).size() == 0) {
                        ArrayList<String> loopStart = new ArrayList<>();
                        loops.get(1).set(0, "task2.preyield1");
                    }
                    switch (loops.get(1).get(0)) { // horizontal
                        case "task2.preyield1":
                            state.put(1, 1); // go to sleep
//                            System.out.println("Task 2 going to sleep preyield");

                            break;
                        case "task2.yield b++":
                            state.put(0, 0); // wake up task1
                            variables.put("b", variables.get("b") + 1);
                            returnvalue.put(1, variables.get("b"));
                            returnstatement.put(1, "task2.preyield2");
                            yielded.put(1, 1);
                            loops.get(0).set(0, "task1.value3");
                            state.put(1, 1); // go to sleep
//                            System.out.println("Task 2 going to sleep");

                            break;
                        case "task2.preyield2":
                            state.put(1, 1); // go to sleep
//                            System.out.println("Task 2 going to sleep preyield");

                            break;
                        case "task2.yield y++":
                            state.put(0, 0); // wake up task1
                            variables.put("y", variables.get("y") + 1);
                            returnvalue.put(1, variables.get("y"));
                            returnstatement.put(1, "task2.while True");
                            yielded.put(1, 1);
                            loops.get(0).set(0, "task1.value1");
                            state.put(1, 1); // go to sleep
//                            System.out.println("Task 2 going to sleep");

                            break;
                    }
                    break;


                case "task3.while True":
                    currentLoop = 0;

                    if (loops.get(2).size() == 0) {
                        ArrayList<String> loopStart = new ArrayList<>();
                        loops.get(2).set(0, "task3.preyield2");
                    }
                    switch (loops.get(2).get(0)) { // horizontal
                        case "task3.preyield2":
                            state.put(2, 1); // go to sleep
//                            System.out.println("Task 3 going to sleep preyield");

                            break;
                        case "task3.yield n++":
                            state.put(0, 0); // wake up task1
                            returnstatement.put(2, "task3.preyield2");
                            variables.put("n", variables.get("n") + 1);
                            returnvalue.put(2, variables.get("n"));
                            state.put(2, 1); // go to sleep
                            yielded.put(2, 2);
//                            System.out.println("Task 3 going to sleep");
                            loops.get(0).set(0, "task1.value4");
                            break;
                        case "task3.preyield1":
                            state.put(2, 1); // go to sleep
//                            System.out.println("Task 3 going to sleep preyield");

                            break;
                        case "task3.yield x++":
                            state.put(0, 0); // wake up task1
                            returnstatement.put(2, "task3.while True");
                            variables.put("x", variables.get("x") + 1);
                            returnvalue.put(2, variables.get("x"));

                            state.put(2, 1); // go to sleep
                            yielded.put(2, 2);
//                            System.out.println("Task 3 going to sleep");
                            loops.get(0).set(0, "task1.value2");
                            break;
                    }
                    break;


            }


            // simple scheduler
            boolean nobody = true;
            for (Map.Entry<Integer, Integer> entry : state.entrySet()) {
                if (entry.getValue() == 0) { // ready to run
                    currenttask = entry.getKey();
                    nobody = false;
                    break;
                } else if (entry.getValue() == 1 &&
                        yielded.containsKey(waitingFor.get(entry.getKey()))
                        && yielded.get(waitingFor.get(entry.getKey())) == 1) {
//                    System.out.println("Found a yield");
                    nobody = false;
                    if (loopYielding.containsKey(entry.getKey())) {
                        // System.out.println("Yielding returning from a loop");
                        // a yield inside a loop
                        loops.get(entry.getKey()).set(loopYielding.get(entry.getKey()), loopreturn.get(entry.getKey()).get(loopYielding.get(entry.getKey())));
                        currenttask = entry.getKey();
                        loopYielding.remove(entry.getKey());
                        loopreturn.get(entry.getKey()).clear();
                        yielded.remove(waitingFor.get(entry.getKey()));
                        waitingFor.remove(entry.getKey());

                    } else {
                        yielded.put(waitingFor.get(entry.getKey()), 0);
                        taskState.set(entry.getKey(), returnstatement.get(entry.getKey()));
                        state.put(entry.getKey(), 0);
                        currenttask = entry.getKey();

                    }
                    break;
                }
            }
            if (nobody) {
                break;
            }



        }
    }
}
