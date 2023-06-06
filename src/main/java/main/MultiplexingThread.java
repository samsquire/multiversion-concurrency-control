package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MultiplexingThread extends Thread implements API {

    private final int id;
    private final MultiplexedAST ast;
    private volatile boolean running = true;
    private Map<String, StateHandler> handlers = new HashMap<>();

    public MultiplexingThread(int id) throws FileNotFoundException, URISyntaxException {
        this.id = id;
        URL res = ClassLoader.getSystemClassLoader().getResource("proper.pipeline");
        File file = Paths.get(res.toURI()).toFile();
        Scanner reader = new Scanner(file);

        StringBuilder programString = new StringBuilder();
        while (reader.hasNextLine()) {
            programString.append("\n").append(reader.nextLine());
        }

        String programString1 = programString.toString();
        System.out.println(programString1);
        MultiplexedAST ast = new MultiplexingProgramParser(programString1).parse();
        System.out.println(ast);
        this.ast = ast;
    }

    public static void main(String[] args) throws FileNotFoundException, URISyntaxException, InterruptedException {

        MultiplexingThread sendThread = new MultiplexingThread(0);
        MultiplexingThread readThread = new MultiplexingThread(1);
        sendThread.setEntryPoint(new Match("thread", "s"));
        sendThread.register("thread", new StateHandler() {

            @Override
            public void handle(API api, MultiplexingProgramParser.Stateline stateline, MultiplexingProgramParser.Identifier identifier) {
                api.fire("yes", "Hello");
            }
        });
        sendThread.register("state1", new StateHandler() {

            @Override
            public void handle(API api, MultiplexingProgramParser.Stateline stateline, MultiplexingProgramParser.Identifier identifier) {
                api.fire("message", "World");
            }
        });
        sendThread.register("state2", new StateHandler() {

            @Override
            public void handle(API api, MultiplexingProgramParser.Stateline stateline, MultiplexingProgramParser.Identifier identifier) {
                api.fire("message", "World");
            }
        });
        sendThread.setEntryPoint(new Match("thread", "r"));
        sendThread.start();
        readThread.start();
        Thread.sleep(5000);
        sendThread.running = false;
        readThread.running = false;
        sendThread.join();
        readThread.join();


    }

    private void register(String state2, StateHandler stateHandler) {
        handlers.put(state2, stateHandler);
    }

    private void setEntryPoint(Match match) {
        MultiplexingProgramParser.Stateline stateline = ast.find(match);
        stateline.runnable = true;
        for (MultiplexingProgramParser.Identifier identifier : stateline.identifiers) {
            for (MultiplexingProgramParser.Fact fact : identifier.arguments) {
                fact.pending++;
            }
            break;
        }
    }

    public void run() {
        while (running) {
            for (MultiplexingProgramParser.Stateline stateline : ast.statelines) {
                if (stateline.runnable) {
                    boolean allsatisfied = true;
                    for (MultiplexingProgramParser.Identifier identifier : stateline.identifiers) {
                        if (identifier.pending()) {
                            System.out.println(String.format("%s is satisfied", identifier));
                            if (handlers.containsKey(identifier.identifier)) {
                                handlers.get(identifier.identifier).handle(this, stateline, identifier);
                            }
                        } else {
                            System.out.println(String.format("%s is NOT satisfied", identifier));
                            allsatisfied = false;
                        }
                        if (!allsatisfied) {
                            break;
                        }
                    }
                    System.out.println(String.format("running %s", stateline));
                }
            }
        }
    }

    @Override
    public void fire(String variable, String value) {
        System.out.println(String.format("firing %s with value %s", variable, value));
        for (MultiplexedAST.Pair pair : ast.variables.get(variable)) {
            pair.fact.pending++;
        }
    }

    public interface StateHandler {
        public void handle(API api,
                           MultiplexingProgramParser.Stateline stateline,
                           MultiplexingProgramParser.Identifier identifier);
    }
}
