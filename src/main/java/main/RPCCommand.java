package main;

public interface RPCCommand {
    RaftThread getSender();
    int getCurrentTerm();
}
