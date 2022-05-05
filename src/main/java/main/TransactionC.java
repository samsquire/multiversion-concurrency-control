package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

class TransactionC extends Thread implements MVCC.Transaction {

    private final MVCC mvcc;
    private final List<MVCC.Transaction> challengers;
    private final List<MVCC.Read> readhandles;
    private boolean aborted = true;
    private volatile int timestamp;
    public List<MVCC.Writehandle> writehandles;
    private volatile boolean cancelled;
    private boolean precommit;
    private boolean restart;
    private int attempts = 0;
    private boolean success;
    private boolean blessed;
    private boolean defeated;
    private ConcurrentHashMap<String, Lock> locks;

    public TransactionC(MVCC mvcc) {
        this.mvcc = mvcc;
        this.writehandles = new ArrayList<>();
        this.challengers = Collections.synchronizedList(new ArrayList<>());
        this.readhandles = new ArrayList<>();
        this.locks = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        super.run();
        while (aborted) {
            while (true) {
                success = true;
                attempts++;
                int previous_timestamp = timestamp;
                timestamp = mvcc.issue(this);

                System.out.println(String.format("Was previously aborted %d previous was %d", timestamp, previous_timestamp));

                MVCC.Read A = mvcc.read(this, "A");
                if (A == null) {
                    success = false;
                    break;
                }
                MVCC.Read B = mvcc.read(this, "B");
                if (B == null) {
                    success = false;
                    break;
                }
                MVCC.Writehandle writeC = mvcc.intend_to_write(this,"A", A.value + 1, A.timestamp);

                if (writeC == null) {
                    success = false;
                    break;
                }

                MVCC.Writehandle writeD = mvcc.intend_to_write(this,"B", B.value + 1, B.timestamp);

                if (writeD == null) {
                    success = false;
                    break;
                }
                mvcc.validate(this);
                mvcc.commit(this);
                mvcc.dump();
                break;


            }
        }
    }

    @Override
    public List<MVCC.Writehandle> getWritehandles() {
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
    public void clear() {
        writehandles.clear();
        cancelled = false;
        challengers.clear();
        readhandles.clear();
        precommit = false;
        restart = false;
        for (Map.Entry<String, Lock> entry : locks.entrySet()) {
            entry.getValue().unlock();
        }
    }

    @Override
    public void addWrite(MVCC.Writehandle writehandle) {
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
    public void addChallenger(MVCC.Transaction transaction) {
        challengers.add(transaction);
    }

    @Override
    public List<MVCC.Transaction> getChallengers() {
        return challengers;
    }

    @Override
    public void addRead(MVCC.Read readHandle) {
        readhandles.add(readHandle);
    }

    @Override
    public List<MVCC.Read> getReadHandles() {
        return readhandles;
    }

    @Override
    public boolean checkChallengers(MVCC mvcc, MVCC.Transaction transaction) {
        synchronized (challengers) {
            for (MVCC.Transaction challenger : challengers) {
                if (mvcc.shouldRestart(transaction, challenger)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String createLockKey() {
        StringBuilder b = new StringBuilder();
        for (MVCC.Writehandle writehandle : writehandles) {
            b.append(writehandle.key);
        }
        return b.toString();
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
    public void addLock(String key, Lock lock) {
        locks.put(key, lock);
    }

    @Override
    public Lock getLock(String key) {
        return locks.get(key);
    }

}
