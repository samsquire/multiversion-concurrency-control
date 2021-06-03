package main;

import java.util.*;

import static java.lang.Integer.min;
import static main.RaftState.*;

public class RaftThread extends Thread {
    int tickCount;
    private final int electionThreshold;
    private int id;
    private static final int MAX_ELECTION_THRESHOLD = 3;
    private static final int MIN_ELECTION_THRESHOLD = 1;

    private RaftState state = Follower;
    private RaftThread currentLeader;
    private boolean leader;
    private int electionTimeout;
    private int currentTerm;
    private int votedFor;
    private List<LogEntry> log = new ArrayList<>();
    private int commitIndex;
    private int lastApplied;
    private Map<Integer, Integer> nextIndex = new HashMap<>();
    private Map<Integer, Integer> matchIndex = new HashMap<>();

    private List<RPCCommand> inbox = new ArrayList<>();
    private boolean down;
    private List<RaftThread> servers;
    private int votes;
    private int votesExpected;
    private Map<RaftThread, AppendEntriesRPC> lastAppendEntriesSent = new HashMap<>();

    public RaftThread(int id, List<RaftThread> servers) {
        this.id = id;
        this.servers = servers;

        electionThreshold = -(new Random().nextInt(MAX_ELECTION_THRESHOLD - MIN_ELECTION_THRESHOLD + 1) + MIN_ELECTION_THRESHOLD);
        electionTimeout = 0;
        System.out.println(String.format("%d %d election threshold", id, electionThreshold));
    }

    public int send(RPCCommand command) {
        int sent = 0;
        for (RaftThread thread : servers) {
            thread.accept(command);
            sent++;
        }
        return sent;
    }

    private void accept(RPCCommand command) {
        inbox.add(command);
    }

    public void handleAllCommand(RaftDriver raftDriver) {
        while (inbox.size() > 0) {
            handleCommand(raftDriver);
        }
    }

    public void handleCommand(RaftDriver driver) {
        if (inbox.size() > 0) {
            RPCCommand command = inbox.get(0);
            inbox.remove(command);
            if (command instanceof RequestVoteRPC) {
                RequestVoteRPC voteRequest = (RequestVoteRPC) command;

                if (log.size() == 0) {
                    this.log.add(new LogEntry(voteRequest.lastLogTerm, null));
                }

                if (voteRequest.lastLogIndex >= log.size() - 1 && voteRequest.lastLogTerm >= currentTerm) {
                    // they win the vote
                    voteRequest.sender.reply(new RequestVoteRPCReply(this, currentTerm, true));
                } else {
                    voteRequest.sender.reply(new RequestVoteRPCReply(this, currentTerm, false));
                }
            }
            if (command instanceof AppendEntriesRPC) {
                electionTimeout = 0;
                state = Follower;

                AppendEntriesRPC appendEntries = (AppendEntriesRPC) command;
                votedFor = appendEntries.getSender().id;
                if (appendEntries.lastLogTerm < currentTerm) {
                    System.out.println("Failed to receive because last Log term < currentTerm");
                    appendEntries.sender.reply(new AppendEntriesRPCReply(this, currentTerm, false));
                    return;
                }
                if (appendEntries.lastLogIndex <= log.size() - 1 &&
                        log.get(appendEntries.lastLogIndex).term != appendEntries.lastLogTerm) {
                    System.out.println("Failed because log index didn't match");
                    appendEntries.sender.reply(new AppendEntriesRPCReply(this, currentTerm, false));
                    return;
                }
                System.out.println(String.format("%d Received %d items", id, appendEntries.logEntries.size()));
                lastApplied = appendEntries.lastLogIndex;
                log.addAll(appendEntries.logEntries);
                if (appendEntries.logEntries.size() > 0 && appendEntries.leaderCommit > commitIndex) {
                    int lastReceived = appendEntries.logEntries.size() - 1;
                    LogEntry lastEntry = appendEntries.logEntries.get(lastReceived);
                    commitIndex = min(appendEntries.leaderCommit, log.indexOf(lastEntry));
                }
                appendEntries.sender.reply(new AppendEntriesRPCReply(this, currentTerm, true));


            }

        }
    }

    private void reply(RPCCommand command) {
        if (command instanceof AppendEntriesRPCReply) {
            AppendEntriesRPCReply reply = (AppendEntriesRPCReply) command;
            if (reply.success) {
                System.out.println("Leader received reply");
                AppendEntriesRPC lastSentAppendEntries = lastAppendEntriesSent.get(command.getSender());
                assert lastSentAppendEntries != null;
                nextIndex.put(command.getSender().id, lastSentAppendEntries.leaderCommit + 1);
                matchIndex.put(command.getSender().id, lastSentAppendEntries.leaderCommit);
                lastAppendEntriesSent.put(command.getSender(), null);

            } else {
                System.out.println("ERROR");
            }
        }
        if (command instanceof RequestVoteRPCReply) {
            RequestVoteRPCReply reply = (RequestVoteRPCReply) command;
            if (reply.voteGranted && votesExpected > 0) {
                votes++;
                if (votes > votesExpected/2) {
                    votesExpected = 0;
                    System.out.println(String.format("%d expects to be the leader now", id));

                        if (log.size() == 0) {
                            this.log.add(new LogEntry(currentTerm, null));
                            commitIndex = 0;
                        }

                        for (RaftThread thread : servers) {
                            if (thread == this) { continue; }
                            // initialize index to send for each server
                            if (!nextIndex.containsKey(thread.id)) {
                                nextIndex.put(thread.id, 1);
                            }
                            if (!matchIndex.containsKey(thread.id)) {
                                matchIndex.put(thread.id, 0);
                            }
                            System.out.println("Initialized server on leader");
                            // send heart beat to end election
                            AppendEntriesRPC firstAppendEntries = new AppendEntriesRPC(this, currentTerm, id,
                                    matchIndex.get(thread.id),
                                    log.get(matchIndex.get(thread.id)).term,
                                    new ArrayList<LogEntry>(), commitIndex);
                            lastAppendEntriesSent.put(thread, firstAppendEntries);
                            thread.accept(firstAppendEntries);

                            leader = true;
                            state = Leader;
                        }
                        votes = 0;
                        votesExpected = 0;


                }
            }
        }
    }

    public void tick() {
        tickCount++;



        if ((state == Follower || state == Candidate) && electionTimeout <= electionThreshold) {
            System.out.println(String.format("%d %d Election timeout. Initiating election", tickCount, id));
            currentTerm++;
            state = Candidate;

            if (log.size() == 0) {
                this.log.add(new LogEntry(currentTerm, null));
                commitIndex = 0;
            }

            votesExpected = send(new RequestVoteRPC(this, currentTerm, id, log.size() - 1, log.get(log.size() - 1).term));

            electionTimeout = 0;
            return;
        } else if (leader) {
            electionTimeout = 0;
            System.out.println("Sending heartbeat");

            updateServers();
        }
        electionTimeout--;

    }


    public Integer save(String newState) {
        if (!leader) {
            return votedFor;
        } else {
            log.add(new LogEntry(currentTerm, newState));
//            for (RaftThread thread : servers) {
//                if (thread == this) { continue; }
//                nextIndex.put(thread.id, matchIndex.get(thread.id) + 1);
//            }
            commitIndex = log.size() - 1;
            updateServers();
            return null;
        }
    }

    public void updateServers() {
        for (RaftThread server : servers) {
            if (server == this) { continue; }
            Integer nextIndex = this.nextIndex.get(server.id);
            Integer knownMatchIndex = matchIndex.get(server.id);
            System.out.println(nextIndex);
            int lastLogterm = log.get(knownMatchIndex).term;

            List<LogEntry> sentLogEntries = log.subList(nextIndex, log.size());

            AppendEntriesRPC command = new AppendEntriesRPC(this,
                    currentTerm, id,
                    knownMatchIndex,
                    lastLogterm, sentLogEntries, commitIndex);
            assert !lastAppendEntriesSent.containsKey(server);
            lastAppendEntriesSent.put(server, command);
            server.accept(command);


        }
    }

    public void dumpLog() {
        for (LogEntry logEntry : log) {
            System.out.println(String.format("%d:%s:%d", id, logEntry.log, logEntry.term));
        }
    }
}
