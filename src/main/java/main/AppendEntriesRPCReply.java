package main;

public class AppendEntriesRPCReply implements RPCCommand {
    private final RaftThread sender;
    private final int currentTerm;
    public final boolean success;
    private int timestamp;

    public AppendEntriesRPCReply(int timestamp, RaftThread sender, int currentTerm, boolean success) {
        this.timestamp = timestamp;
        this.sender = sender;
        this.currentTerm = currentTerm;
        this.success = success;
    }

    @Override
    public RaftThread getSender() {
        return sender;
    }

    @Override
    public int getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }
}
