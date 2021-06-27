package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    public TransactionC(MVCC mvcc) {
        this.mvcc = mvcc;
        this.writehandles = new ArrayList<>();
        this.challengers = Collections.synchronizedList(new ArrayList<>());
        this.readhandles = new ArrayList<>();

    }

    @Override
    public void run() {
        super.run();
        while (aborted) {
            while (true) {
                int previous_timestamp = timestamp;
                timestamp = mvcc.issue(this);

                System.out.println(String.format("Was previously aborted %d previous was %d", timestamp, previous_timestamp));

                MVCC.Read A = mvcc.read(this, "A");
                if (A == null) {
                    break;
                }
                MVCC.Read B = mvcc.read(this, "B");
                if (B == null) {
                    break;
                }
                MVCC.Writehandle writeC = mvcc.intend_to_write(this,"A", A.value + 1, A.timestamp);

                if (writeC == null) {
                    break;
                }

                MVCC.Writehandle writeD = mvcc.intend_to_write(this,"B", B.value + 1, B.timestamp);

                if (writeD == null) {
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
    public boolean checkChallengers(MVCC.Transaction transaction) {
        synchronized (challengers) {
            for (MVCC.Transaction challenger : challengers) {
                if (challenger.getTimestamp() < transaction.getTimestamp()) {
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
}
