package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

public class InterpreterRunner {
    private HashMap<String, InstructionHandler> instructionHandlers;
    private HashMap<String, Integer> variables = new HashMap<String, Integer>();

    public InterpreterRunner() {
        this.instructionHandlers = new HashMap<>();
    }

    public static void main(String[] args) {
        new InterpreterRunner().run();
    }

    public void registerInstruction(String name, InstructionHandler instruction) {
        instructionHandlers.put(name, instruction);
    }

    private void run() {
        try {
            long start = System.currentTimeMillis();
            URL res = getClass().getClassLoader().getResource("crossthread-nosend.pint");
            File file = Paths.get(res.toURI()).toFile();
            String absolutePath = file.getAbsolutePath();
            Scanner reader = new Scanner(file);

            HashMap<String, Map<String, String>> lookups = new HashMap<>();
            registerInstruction("threads", new ThreadsInstruction());
            registerInstruction("set", new SetInstruction());
            registerInstruction("while", new WhileInstruction());
            registerInstruction("receive", new ReceiveInstruction());
            registerInstruction("add", new AddInstruction());
            registerInstruction("modulo", new ModuloInstruction());
            registerInstruction("send", new SendInstruction());
            registerInstruction("endwhile", new EndWhileInstruction());
            registerInstruction("addv", new AddValueInstruction());
            registerInstruction("println", new PrintLnInstruction());
            registerInstruction("sendcode", new SendCodeInstruction());
            registerInstruction("receivecode", new ReceiveCodeInstruction());
            registerInstruction("mailbox", new MailboxInstruction());
            registerInstruction("return", new ReturnInstruction());
            registerInstruction("jump", new JumpInstruction());
            Map<String, Integer> labels = new HashMap<>();
            List<Map<String, String>> program = new ArrayList<>();
            boolean programStarted = false;
            List<String> programInstructionTypes = new ArrayList<>();
            int pc = 0;
            int programStart = 0;
            HashMap<String, String> empty = new HashMap<>();
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                int nextPc = pc + 1;
                System.out.println(data);
                if (data.length() == 0) {
                    programInstructionTypes.add("");
                    program.add(empty);
                    continue; }
                if (data.charAt(0) == ':') {
                    // label
                    labels.put(data.substring(1, data.length()), pc);
                    programInstructionTypes.add(data);
                    program.add(empty);
                }
                else if (data.equals("<start>")) {
                    programStarted = true;
                    programStart = nextPc;
                    programInstructionTypes.add("<start>");
                    program.add(empty);
                } else {
                    String[] args = data.split(" ");

                    List<String> argsList = Arrays.asList(args);
                    String instructionName = argsList.get(0);
                    programInstructionTypes.add(instructionName);
                    Map<String, String> parsed = instructionHandlers.get(instructionName).run(variables, argsList);
                    lookups.put(instructionName, parsed);

                    program.add(parsed);

                }
                pc = nextPc;
            }
            System.out.println(programInstructionTypes);
            System.out.println(labels);
            int threadCount = Integer.parseInt(lookups.get("threads").get("threads"));
            List<Interpreter> interpreters = new ArrayList<>();
            int mailboxes = 10;
            int messageRate = 3000000;
            int numSubthreads = 10;
            ArrayList<ArrayList<Interpreter.AlternativeMessage>> messages = new ArrayList<>();
            int threadNum = 0;
            int totalSize = threadCount;
            for (int i = 0; i < threadCount; i++) {
                System.out.println(String.format("Creating interpreter thread %d", i));

                interpreters.add(new Interpreter(messages, 0, messageRate, threadNum++, i, new ArrayList<>(), totalSize, false, mailboxes, numSubthreads, programInstructionTypes, program, programStart, variables, labels));
            }
            for (Interpreter interpreter : interpreters) {
                interpreter.setThreads(new ArrayList<>(interpreters));
            }
            for (Interpreter interpreter : interpreters) {
                interpreter.start();
            }
            Thread.sleep(5000);
            for (Interpreter interpreter : interpreters) {
                interpreter.running = false;
            }
            for (Interpreter interpreter : interpreters) {
                interpreter.variables.put("running", 0);
            }
            for (Interpreter interpreter : interpreters) {
                interpreter.join();
            }
            long totalRequests = 0;
            for (Interpreter thread : interpreters) {
                totalRequests += thread.variables.get("current");
            }
            long end = System.currentTimeMillis();
            double seconds = (end - start) / 1000.0;
            System.out.println(String.format("%d total requests", totalRequests));
            double l = totalRequests / seconds;
            System.out.println(String.format("%f requests per second", l));
            System.out.println(String.format("Time taken: %f", seconds));

            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class ThreadsInstruction implements InstructionHandler {

        @Override
        public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
            String threadCount = args.get(1);

            HashMap<String, String> parsed = new HashMap<>();
            parsed.put("threads", threadCount);
            return parsed;

        }
    }
}
