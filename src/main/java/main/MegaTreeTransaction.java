package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MegaTreeTransaction<V> extends Thread implements MegaTree.Transaction<V> {
    private final int size;
    private int threadIndex;
    private MegaTree<V> megatree;


    private List<MegaTree.Transaction> challengers;
    private List<MegaTree.Read> readhandles;
    private boolean aborted = true;
    private volatile int timestamp;
    public List<MegaTree<V>.Writehandle> writehandles;
    private volatile boolean cancelled;
    private boolean precommit;
    private boolean restart;
    private int attempts = 0;
    private boolean success;
    private boolean blessed;
    private boolean defeated;
    private LRHashMap<String, ReentrantReadWriteLock.WriteLock> locks;
    private int version;

    public MegaTreeTransaction(MegaTree<V> megatree, int threadIndex, int size) {
        this.threadIndex = threadIndex;
        this.megatree = megatree;
        this.writehandles = Collections.synchronizedList(new ArrayList<>());
        this.readhandles = Collections.synchronizedList(new ArrayList<>());
        this.challengers = Collections.synchronizedList(new ArrayList<>());
        this.size = size;
        this.locks = new LRHashMap<>(size);
    }

    public void run() {
        try {
            while (aborted) {
                while (true) {
                    timestamp = megatree.issue(threadIndex, this);
                    System.out.println(String.format("%d timestamp restarting", timestamp));
                    MegaTree<V>.Read<MegaTree<V>.Version<V>> lastCounter = megatree.get(threadIndex, "a", this);
                    if (lastCounter == null) {
                        break;
                    }
                    MegaTree<V>.Version<V> lastVersion = lastCounter.value;

                    Integer value = (Integer) lastVersion.value;

                    megatree.put(threadIndex, this, "a", (V) new Integer(value + 1), lastCounter.timestamp);
                    megatree.commit(threadIndex, this);
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    @Override
    public List<MegaTree<V>.Writehandle> getWritehandles() {
        return writehandles;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    @Override
    public void clear(int threadIndex, MegaTree MegaTree) {
        writehandles.clear();
        cancelled = false;
        challengers.clear();
        readhandles.clear();
        precommit = false;
        restart = false;

        locks.clear();

    }

    @Override
    public void addWrite(MegaTree.Writehandle writehandle) {
        writehandles.add(writehandle);
    }

    @Override
    public boolean getAborted() {
        return aborted;
    }

    @Override
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean getCancelled() {
        return cancelled;
    }

    @Override
    public void addChallenger(MegaTree.Transaction transaction) {
        assert(transaction != null);
        challengers.add(transaction);

    }

    @Override
    public List<MegaTree.Transaction> getChallengers() {
        return challengers;
    }

    @Override
    public void addRead(MegaTree.Read readHandle) {
        readhandles.add(readHandle);
    }

    @Override
    public List<MegaTree.Read> getReadHandles() {
        return readhandles;
    }

    @Override
    public boolean checkChallengers(MegaTree megaTree, MegaTree.Transaction transaction) {

        synchronized (challengers) {
            for (MegaTree.Transaction challenger : challengers) {
                assert (challenger != null);
                if (megaTree.shouldRestart(transaction, challenger)) {
                    return true;
                }
            }
        }


        return false;
    }

    @Override
    public void markPrecommit() {
        precommit = true;
    }

    @Override
    public boolean getPrecommit() {
        return precommit;
    }


    @Override
    public void markRestart(boolean restart) {
        this.restart = restart;
    }

    @Override
    public boolean getRestart() {
        return restart;
    }

    public int getNumberOfAttempts() {
        return attempts;
    }

    @Override
    public void markSuccessful() {
        success = true;
    }

    @Override
    public boolean getSuccessful() {
        return success;
    }

    @Override
    public void addLock(int threadIndex, String key, ReentrantReadWriteLock.WriteLock lock) {
        locks.put(key, lock, timestamp);
    }

    @Override
    public ReentrantReadWriteLock.WriteLock getLock(int threadIndex, String key) {
        return locks.get(new LRHashMap.Reader(threadIndex), key);
    }
}
