package main;

public class RequestVoteRPCReply implements RPCCommand {
    private RaftThread sender;
    private final int currentTerm;
    public final boolean voteGranted;

    public RequestVoteRPCReply(RaftThread sender, int currentTerm, boolean voteGranted) {
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
}
