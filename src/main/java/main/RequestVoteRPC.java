package main;

public class RequestVoteRPC implements RPCCommand {
    public final RaftThread sender;
    private final int currentTerm;
    private final int term;
    private final int candidateId;
    public final int lastLogIndex;
    public final int lastLogTerm;

    public RequestVoteRPC(RaftThread sender, int currentTerm, int candidateId, int lastLogIndex, int lastLogTerm) {
        this.sender = sender;
        this.currentTerm = currentTerm;
        this.term = currentTerm;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public RaftThread getSender() {
        return sender;
    }

    @Override
    public int getCurrentTerm() {
        return currentTerm;
    }

}
