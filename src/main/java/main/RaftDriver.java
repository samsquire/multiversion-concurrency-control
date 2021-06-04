package main;

import java.util.ArrayList;
import java.util.List;

public class RaftDriver {
    private List<RaftThread> servers;
    private int tickCount;
    public static void main(String[] args) {
        new RaftDriver().run();
    }

    private void run() {
        int ids = 0;
        servers = new ArrayList<>();

        for (int i = 0 ; i < 5 ; i++) {
            RaftThread raftThread = new RaftThread(ids, servers);
            raftThread.start();
            servers.add(raftThread);
            ids++;
        }
        // give some time for a leader to be elected (maximum 3 ticks)
        tickAll();
        tickAll();
        tickAll();
        tickAll();
        save("Hello world");
        tickAll();
        save("Sam");
        tickAll();
        tickAll();
        tickAll();
        RaftThread thread = pullLeaderDown();
        tickAll();
        tickAll();
        tickAll();
        tickAll();
        assert getLeaders().size() == 1;
        save("new leader");
        thread.markUp();
        assert getLeaders().size() == 1;
        tickAll();
        tickAll();
        tickAll();
        assert getLeaders().size() == 1;
        printLog();


    }

    private RaftThread pullLeaderDown() {
        for (RaftThread server : servers) {
            if (server.leader) {
                server.markDown();
                return server;
            }
        }
        return null;
    }

    private List<RaftThread> getLeaders() {
        List<RaftThread> leaders = new ArrayList<>();
        for (RaftThread thread : servers) {
            if (thread.leader) {
                leaders.add(thread);
            }
        }
        return leaders;
    }

    private void printLog() {
        for (RaftThread thread : servers) {
            thread.dumpLog();
        }
    }

    private void save(String state) {
        Integer leaderIndex = servers.get(0).save(state);
        if (leaderIndex != null) {
            servers.get(leaderIndex).save(state);
            for (RaftThread thread : servers) {

                handleAllMessages();
            }
        }
    }

    private void tickAll() {
        tickCount++;
        System.out.println(String.format("%d TICK", tickCount));
        for (RaftThread thread : servers) {
            handleAllMessages();
            thread.tick();
            handleAllMessages();
        }
    }


    private void handleMessages() {
        for (RaftThread thread : servers) {
            thread.handleCommand(this);
        }
    }

    private void handleAllMessages() {
        for (RaftThread thread : servers) {
            thread.handleAllCommand(this);
        }
    }
}
