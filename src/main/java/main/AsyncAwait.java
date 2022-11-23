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
        HashMap<Integer, String> returnvalue = new HashMap<>();
        List<String> taskState = new ArrayList<>();
        taskState.add("task1.print(\"Starting task1\")"); // task1
        taskState.add("task2.print(\"Starting task2\")"); // task2
        taskState.add("task3.print(\"Starting task3\")"); // task3
        HashMap<Integer, ArrayList<String>> loops = new HashMap<>();
        HashMap<Integer, Map<Integer, String>> loopreturn = new HashMap<>();
        ArrayList<String> innerloops = new ArrayList<>();
        innerloops.add("task1.value1 = await task2()");
        loops.put(0, innerloops);
        loopreturn.put(0, new HashMap<>());

        innerloops = new ArrayList<>();
        innerloops.add("task2.preyield");
        loops.put(1, innerloops);
        loopreturn.put(1, new HashMap<>());

        innerloops = new ArrayList<>();
        innerloops.add("task3.preyield");
        loops.put(2, innerloops);
        loopreturn.put(2, new HashMap<>());

        HashMap<Integer, String> returnstatement = new HashMap<>();
        HashMap<String, String> variables = new HashMap<>();
        int currenttask = 0;
        int currentLoop = 0;
        HashMap<Integer, Integer> loopYielding = new HashMap<>();



        while (true) { // vertical
//            System.out.println(currenttask);
//            System.out.println(taskState.get(currenttask));
//            state.put(currenttask, 1); // block
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
                        case "task1.value1 = await task2()":
                            state.put(1, 0); // wake up task2
                            waitingFor.put(0, 1);
                            returnstatement.put(0, "task1.while True");
                            loops.get(1).set(0, "task2.yield \"Hello\"");
                            state.put(0, 1);
                            break;
                        case "task1.value1":
                            String value = returnvalue.get(1);
//                            System.out.println(String.format("Setting value1 to %s", value));
                            variables.put("value1", value);
                            state.put(0, 0);
                            loops.get(0).set(0, "task1.value2 = await task3()");
                            returnstatement.put(0, "task1.while True");

                            break;
                        case "task1.value2":
//                            System.out.println(String.format("Setting value2 %s", returnvalue.get(2)));

                            variables.put("value2", returnvalue.get(2));
                            returnstatement.put(0, "task1.while True");
                            state.put(0, 0);
                            loops.get(0).set(0, "task1.print(value1, value2)");
                            break;
                        case "task1.value2 = await task3()":
                            state.put(2, 0); // wake up task3
                            waitingFor.put(0, 2);
                            returnstatement.put(0, "task1.while True");
                            state.put(0, 1);
                            loopreturn.get(0).put(0, "task1.print(value1, value2)");
                            loops.get(2).set(0, "task3.yield \"World\"");

                            break;
                        case "task1.print(value1, value2)":
                            System.out.println(String.format("%s %s", variables.get("value1"), variables.get("value2")));
                            returnstatement.put(0, "task1.while True");
                            loops.get(0).set(0, "task1.value1 = await task2()");
                            state.put(0, 0);

                    }
                    break;


                case "task2.while True":
                    currentLoop = 1;
//                    System.out.println(loops.get(1).get(0));

                    if (loops.get(1).size() == 0) {
                        ArrayList<String> loopStart = new ArrayList<>();
                        loops.get(1).set(0, "task2.yield \"Hello\"");
                    }
                    switch (loops.get(1).get(0)) { // horizontal
                        case "task2.preyield":
                            state.put(1, 1); // go to sleep
//                            System.out.println("Task 2 going to sleep preyield");

                            break;
                        case "task2.yield \"Hello\"":
                            state.put(0, 0); // wake up task1
                            returnvalue.put(1, "Hello");
                            returnstatement.put(1, "task2.while True");
                            yielded.put(1, 1);
                            loops.get(0).set(0, "task1.value1");
                            state.put(1, 1); // go to sleep
//                            System.out.println("Task 2 going to sleep");
                            loopYielding.put(0, 0);

                            break;
                    }
                    break;


                case "task3.while True":
                    currentLoop = 0;

                    if (loops.get(2).size() == 0) {
                        ArrayList<String> loopStart = new ArrayList<>();
                        loops.get(2).set(0, "task3.yield \"World\"");
                    }
                    switch (loops.get(2).get(0)) { // horizontal
                        case "task3.preyield":
                            state.put(2, 1); // go to sleep
//                            System.out.println("Task 3 going to sleep preyield");

                            break;
                        case "task3.yield \"World\"":
                            state.put(0, 0); // wake up task1
                            returnstatement.put(1, "task2.while True");
                            returnvalue.put(2, "World");

                            state.put(2, 1); // go to sleep
                            yielded.put(2, 1);
//                            System.out.println("Task 3 going to sleep");
                            loops.get(0).set(0, "task1.value2");
                            loopYielding.put(0, 0);
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
                        yielded.containsKey(waitingFor.get(entry.getKey())) && yielded.get(waitingFor.get(entry.getKey())) == 1) {
//                    System.out.println("Found a yield");
                    nobody = false;
                    if (loopYielding.containsKey(entry.getKey())) {
                        // System.out.println("Yielding returning from a loop");
                        // a yield inside a loop
                        loops.get(entry.getKey()).set(loopYielding.get(entry.getKey()), loopreturn.get(entry.getKey()).get(loopYielding.get(entry.getKey())));
                        currenttask = entry.getKey();
                        loopYielding.remove(entry.getKey());

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
