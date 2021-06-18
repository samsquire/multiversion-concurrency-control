package main;

public class RequestVoteRPCReply implements RPCCommand {
    private int timestamp;
    private RaftThread sender;
    private final int currentTerm;
    public final boolean voteGranted;

    public RequestVoteRPCReply(int timestamp, RaftThread sender, int currentTerm, boolean voteGranted) {
        this.timestamp = timestamp;
        this.sender = sender;
        this.currentTerm = currentTerm;
        this.voteGranted = voteGranted;
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
