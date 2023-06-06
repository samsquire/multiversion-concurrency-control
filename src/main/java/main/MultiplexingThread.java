package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiplexingThread extends Thread implements API {

    private final int id;
    private final MultiplexedAST ast;
    private final ReentrantReadWriteLock.WriteLock lock;
    private volatile boolean running = true;
    private Map<String, StateHandler> handlers = new HashMap<>();
    private Map<String, StateHandler> internal = new HashMap<>();
    private ArrayList<MultiplexingThread> threads;

    public MultiplexingThread(int id) throws FileNotFoundException, URISyntaxException {
        this.id = id;
        this.lock = new ReentrantReadWriteLock().writeLock();
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

    public static void main(String[] args)
            throws FileNotFoundException,
            URISyntaxException,
            InterruptedException {

        List<MultiplexingThread> threads = new ArrayList<>();
        MultiplexingThread sendThread = new MultiplexingThread(0);
        MultiplexingThread readThread = new MultiplexingThread(1);
        threads.add(sendThread);
        threads.add(readThread);
        sendThread.setThreads(new ArrayList<>(threads));
        readThread.setThreads(new ArrayList<>(threads));
        sendThread.setEntryPoint(new Match("thread", "s"));
        sendThread.register("thread", new StateHandler() {

            @Override
            public void handle(API api,
                               MultiplexingProgramParser.Stateline stateline,
                               MultiplexingProgramParser.Identifier identifier,
                               List<MultiplexingProgramParser.Fact> values) {
                Map<String, String> valueMap = api.createValueMap(values);
                api.getAst().variables.get(identifier.identifier);

                api.fire("state1", valueMap);
            }
        });
        readThread.register("thread", new StateHandler() {

            @Override
            public void handle(API api,
                               MultiplexingProgramParser.Stateline stateline,
                               MultiplexingProgramParser.Identifier identifier,
                               List<MultiplexingProgramParser.Fact> values) {
                Map<String, String> valueMap = api.createValueMap(values);
                api.getAst().variables.get(identifier.identifier);

                api.fire("state1", valueMap);
            }
        });
        sendThread.register("state1", new StateHandler() {

            @Override
            public void handle(API api,
                               MultiplexingProgramParser.Stateline stateline,
                               MultiplexingProgramParser.Identifier identifier,
                               List<MultiplexingProgramParser.Fact> values) {
                Map<String, String> valueMap = api.createValueMap(values);
                api.wait("send");
                api.submit("send", "message", "Hello world");
                for (MultiplexingProgramParser.Fact fact : values) {
                    fact.submitted++;
                }

                api.fire("receive", valueMap);
            }
        });
        readThread.register("receive", new StateHandler() {

            @Override
            public void handle(API api,
                               MultiplexingProgramParser.Stateline stateline,
                               MultiplexingProgramParser.Identifier identifier,
                               List<MultiplexingProgramParser.Fact> values) {
                Map<String, String> valueMap = api.createValueMap(values);
                System.out.println("read thread setting message2");
                api.wait("send");
                api.submit("send", "message2", "Hello reply");
                api.fire("receive", valueMap);
            }
        });
        readThread.setEntryPoint(new Match("thread", "r"));
        sendThread.start();
        readThread.start();
        Thread.sleep(5000);
        sendThread.running = false;
        readThread.running = false;
        sendThread.join();
        readThread.join();


    }

    private void setThreads(ArrayList<MultiplexingThread> threads) {
        this.threads = threads;
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
        System.out.println(String.format("%d Thread started", id));
        register("send", new StateHandler() {

            @Override
            public void handle(API api,
                               MultiplexingProgramParser.Stateline stateline,
                               MultiplexingProgramParser.Identifier identifier,
                               List<MultiplexingProgramParser.Fact> values) {
                System.out.println(String.format("SEND %s %s", identifier, values));
                Map<String, String> valueMap = api.createValueMap(values);
                for (MultiplexingThread thread : threads) {
                    for (MultiplexingProgramParser.Fact fact : values) {
                        System.out.println("FACT2 " + fact);
                        fact.pending++;
                    }
                    thread.lock.lock();

                    thread.fire("receive", valueMap);

                    thread.lock.unlock();
                }
            }
        });
        for (MultiplexedAST.Pair pair : ast.children.get("thread")) {
            pair.fact.values.add(String.valueOf(id));
        }
        while (running) {
            for (MultiplexingProgramParser.Stateline stateline : ast.statelines) {
                if (stateline.runnable) {
                    boolean allsatisfied = true;
                    for (MultiplexingProgramParser.Identifier identifier : stateline.identifiers) {
                        if (identifier.pending()) {
                            System.out.println(String.format("%d %s is satisfied", id, identifier));
                            if (handlers.containsKey(identifier.identifier)) {

                                handlers.get(identifier.identifier).handle(this, stateline, identifier, identifier.arguments);
                            }
                            if (internal.containsKey(identifier.identifier)) {

                            }
                        } else {
                            System.out.println(String.format("%d %s is NOT satisfied", id, identifier));
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
    public void fire(String identifier,
                     Map<String, String> values) {
        System.out.println(String.format("%d firing %s with value %s", id, identifier, values));
        for (MultiplexedAST.Pair pair : ast.children.get(identifier)) {
            pair.fact.submit(values.get(pair.fact.name));
            pair.fact.pending++;
        }
    }

    @Override
    public MultiplexedAST getAst() {
        return ast;
    }

    @Override
    public Map<String, String> createValueMap(List<MultiplexingProgramParser.Fact> values) {
        HashMap<String, String> valueMap = new HashMap<>();
        for (MultiplexingProgramParser.Fact fact : values) {
            valueMap.put(fact.name, fact.values.get(fact.values.size() - 1));
        }
        return valueMap;
    }

    @Override
    public void wait(String send) {
        for (MultiplexedAST.Pair pair : ast.children.get(send)) {
            pair.fact.pending++;
        }
    }

    @Override
    public void submit(String identifier, String fact, String value) {
        for (MultiplexedAST.Pair pair : ast.children.get(identifier)) {
            if (pair.fact.name.equals(fact)) {
                pair.fact.submit(value);
            }
        }
    }

    public interface StateHandler {
        public void handle(API api,
                           MultiplexingProgramParser.Stateline stateline,
                           MultiplexingProgramParser.Identifier identifier,
                           List<MultiplexingProgramParser.Fact> value);
    }
}
