package main;

import java.util.ArrayList;
import java.util.List;

public class AppendEntriesRPC implements RPCCommand {
    public RaftThread sender;
    private final int currentTerm;
    private final int leaderId;
    public final int lastLogIndex;
    public final int lastLogTerm;
    public final List<LogEntry> logEntries;
    public final int leaderCommit;
    private int timestamp;

    public AppendEntriesRPC(int timestamp, RaftThread sender, int currentTerm, int leaderId, int lastLogIndex, int lastLogTerm, List<LogEntry> logEntries, int leaderCommit) {
        this.sender = sender;
        this.currentTerm = currentTerm;
        this.leaderId = leaderId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
        this.logEntries = logEntries;
        this.leaderCommit = leaderCommit;
        this.timestamp = timestamp;
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
