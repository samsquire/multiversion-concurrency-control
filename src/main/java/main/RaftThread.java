package main;

import java.util.*;

import static java.lang.Integer.min;
import static main.RaftState.*;

public class RaftThread extends Thread {
    int tickCount;
    private final int electionThreshold;
    public int id;
    private static final int MAX_ELECTION_THRESHOLD = 3;
    private static final int MIN_ELECTION_THRESHOLD = 1;

    private RaftState state = Follower;
    private RaftThread currentLeader;
    public boolean leader;
    private int electionTimeout;
    private int currentTerm;
    private int votedFor;
    private List<LogEntry> log = new ArrayList<>();
    private int commitIndex;
    private int lastApplied;
    private Map<Integer, Integer> nextIndex = new HashMap<>();
    private Map<Integer, Integer> matchIndex = new HashMap<>();

    private List<RPCCommand> inbox = new ArrayList<>();
    public boolean down;
    private List<RaftThread> servers;
    private int votes;
    private int votesExpected;
    private Map<RaftThread, AppendEntriesRPC> lastAppendEntriesSent = new HashMap<>();
    private Map<RaftThread, Integer> lastTimestamp = new HashMap<>();
    private int timestamp;

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
            timestamp++;
            thread.accept(command);
            sent++;

        }
        return sent;
    }

    private void accept(RPCCommand command) {
        if (down) { return; }
        inbox.add(command);
    }

    public void handleAllCommand(RaftDriver raftDriver) {
        if (down) { return; }
        while (inbox.size() > 0) {
            handleCommand(raftDriver);
        }
    }

    public void handleCommand(RaftDriver driver) {
        if (down) { return; }

        if (inbox.size() > 0) {
            timestamp++;
            RPCCommand command = inbox.get(0);
            inbox.remove(command);
            if (!handleTimestamp(command)) { return; }
            if (command.getCurrentTerm() > currentTerm) {
                if (leader) {
                    System.out.println(String.format("%d Stepping down", id));
                }
                leader = false;
                state = Follower;
                System.out.println(String.format("Received message with higher term %d > %d", command.getCurrentTerm(), currentTerm));
                currentTerm = command.getCurrentTerm();
            } else if (command.getCurrentTerm() < currentTerm) {
                System.out.println("Discarding old message");
                return;
            }

            if (command instanceof RequestVoteRPC) {
                RequestVoteRPC voteRequest = (RequestVoteRPC) command;
                System.out.println(String.format("%d %d Voting", tickCount, id));
                if (log.size() == 0) {
                    this.log.add(new LogEntry(voteRequest.lastLogTerm, null));
                }

                if (voteRequest.lastLogIndex >= log.size() - 1) {
                    // they win the vote
                    System.out.println("They win the vote");
                    votedFor = voteRequest.sender.id;
                    voteRequest.sender.reply(new RequestVoteRPCReply(timestamp,this, currentTerm, true));
                } else {
                    System.out.println("They lose the vote");
                    voteRequest.sender.reply(new RequestVoteRPCReply(timestamp,this, currentTerm, false));
                }
            }
            if (command instanceof AppendEntriesRPC) {
                electionTimeout = 0;
                state = Follower;
                leader = false;

                AppendEntriesRPC appendEntries = (AppendEntriesRPC) command;
                votedFor = appendEntries.getSender().id;
                if (appendEntries.lastLogTerm != log.get(log.size() - 1).term) {
                    System.out.println("Failed to receive because last Log term < currentTerm");
                    appendEntries.sender.reply(new AppendEntriesRPCReply(timestamp, this, currentTerm, false));
                    return;
                }
                if (appendEntries.lastLogIndex <= log.size() - 1 &&
                        log.get(appendEntries.lastLogIndex).term != appendEntries.lastLogTerm) {
                    System.out.println("Failed because log index didn't match");
                    appendEntries.sender.reply(new AppendEntriesRPCReply(timestamp,this, currentTerm, false));
                    return;
                }
                System.out.println(String.format("%d Received %d items", id, appendEntries.logEntries.size()));
                lastApplied = appendEntries.lastLogIndex;
                log.addAll(appendEntries.logEntries);
                if (appendEntries.leaderCommit > commitIndex) {
                    if (appendEntries.logEntries.size() == 0) {
                        commitIndex = log.size() - 1;
                    } else {
                        int lastReceived = appendEntries.logEntries.size() - 1;
                        LogEntry lastEntry = appendEntries.logEntries.get(lastReceived);
                        commitIndex = min(appendEntries.leaderCommit, log.indexOf(lastEntry));
                    }
                }
                appendEntries.sender.reply(new AppendEntriesRPCReply(timestamp, this, currentTerm, true));


            }

        }
    }

    private boolean handleTimestamp(RPCCommand command) {
        if (lastTimestamp.containsKey(command.getSender())) {
            int existingTimestamp = lastTimestamp.get(command.getSender());

            if (command.getTimestamp() > existingTimestamp) {
                lastTimestamp.put(command.getSender(), command.getTimestamp());
                return true;
            } else {
                System.out.println(String.format("Old timestamp was %d new message timestamp was %d", existingTimestamp, command.getTimestamp()));
                System.out.println("Disregarding old timestamp message");
                return false;
            }
        } else {
            lastTimestamp.put(command.getSender(), command.getTimestamp());
            return true;
        }
    }

    private void reply(RPCCommand command) {
        if (down) { return; }
        timestamp++;
        if (!handleTimestamp(command)) { return; }
        if (command.getCurrentTerm() > currentTerm) {
            if (leader) {
                System.out.println(String.format("%d Stepping down", id));
            }
            leader = false;
            state = Follower;
            System.out.println(String.format("Received message with higher term %d > %d", command.getCurrentTerm(), currentTerm));
            currentTerm = command.getCurrentTerm();
        } else if (command.getCurrentTerm() < currentTerm) {
            System.out.println("Discarding old reply");
            return;
        }
        if (command instanceof AppendEntriesRPCReply) {
            AppendEntriesRPCReply reply = (AppendEntriesRPCReply) command;
            if (reply.success) {
                // System.out.println("Leader received reply");
                AppendEntriesRPC lastSentAppendEntries = lastAppendEntriesSent.get(reply.getSender());
                assert lastSentAppendEntries != null;
                assert command.getSender() != null;
                assert nextIndex != null;
                assert matchIndex != null;
                // System.out.println(String.format("%d is the leaderCommit", lastSentAppendEntries.leaderCommit));

                nextIndex.put(reply.getSender().id, lastSentAppendEntries.leaderCommit + 1);
                matchIndex.put(reply.getSender().id, lastSentAppendEntries.leaderCommit);
                lastAppendEntriesSent.put(lastSentAppendEntries.getSender(), null);

            } else {
                System.out.println("ERROR");
            }
        }
        if (command instanceof RequestVoteRPCReply) {
            RequestVoteRPCReply reply = (RequestVoteRPCReply) command;
            if (reply.voteGranted && votesExpected > 0) {
                votes++;
                // System.out.println("Received vote");
                if (votes >= votesExpected/2) {
                    votesExpected = 0;
                    votes = 0;
                    System.out.println(String.format("%d expects to be the leader now", id));

                        if (log.size() == 0) {
                            this.log.add(new LogEntry(currentTerm, null));
                            commitIndex = 0;
                        }

                        for (RaftThread thread : servers) {
                            if (thread == this) { continue; }
                            // initialize index to send for each server

                            nextIndex.put(thread.id, commitIndex + 1);


                            matchIndex.put(thread.id, commitIndex);

                            System.out.println("Initialized server on leader");
                            // send heart beat to end election
                            AppendEntriesRPC firstAppendEntries = new AppendEntriesRPC(timestamp, this, currentTerm, id,
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
        timestamp++;
        tickCount++;
        if (down) { return; }


        if ((state == Follower || state == Candidate) && electionTimeout <= electionThreshold) {
            System.out.println(String.format("%d %d Election timeout. Initiating election", tickCount, id));
            currentTerm++;
            state = Candidate;

            if (log.size() == 0) {
                this.log.add(new LogEntry(currentTerm, null));
                commitIndex = 0;
            }

            votesExpected = send(new RequestVoteRPC(timestamp, this, currentTerm, id, commitIndex, log.get(log.size() - 1).term));

            electionTimeout = 0;
            return;
        } else if (leader) {
            electionTimeout = 0;
            System.out.println("Sending heartbeat");

            updateServers();
        } else {
            electionTimeout--;
        }


    }


    public Integer save(String newState) {
        if (!leader) {
            return votedFor;
        } else {
            timestamp++;
            System.out.println(String.format("%d Leader saved %s", id, newState));
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
            // System.out.println(String.format("Next index is %d", nextIndex));
            int lastLogterm = log.get(knownMatchIndex).term;

            List<LogEntry> sentLogEntries = new ArrayList<>(log.subList(nextIndex, log.size()));

            AppendEntriesRPC command = new AppendEntriesRPC(timestamp, this,
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

    public void markDown() {
        down = true;
    }

    public void markUp() {
        down = false;
    }
}
