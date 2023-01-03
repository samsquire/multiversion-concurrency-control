package main;

public class LoopEnvironment {
    private final int task;
    public final int loop;
    public final String instruction;

    public LoopEnvironment(int task, int loop, String instruction) {
        this.task = task;
        this.loop = loop;
        this.instruction = instruction;
    }
}
