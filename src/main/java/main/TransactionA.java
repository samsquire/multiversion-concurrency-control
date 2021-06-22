package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class TransactionA extends Thread implements MVCC.Transaction {

    private final MVCC mvcc;
    private final ConcurrentHashMap<String, Integer> rts;
    private boolean aborted = true;
    private volatile int timestamp;
    public List<MVCC.Writehandle> writehandles;
    private MVCC.Transaction challenger;

    public TransactionA(MVCC mvcc) {
        this.mvcc = mvcc;
        this.writehandles = new ArrayList<>();
        this.rts = new ConcurrentHashMap<String, Integer>();

    }

    @Override
    public void run() {
        super.run();
        while (aborted) {
            while (true) {
                timestamp = mvcc.issue(this);

                System.out.println(String.format("Was previously aborted %d", timestamp));
                MVCC.Writehandle writeA = mvcc.intend_to_write(this,"A", 1, null);

                if (writeA == null) {
                    break;
                }
                MVCC.Writehandle writeB = mvcc.intend_to_write(this,"B", 1, null);

                if (writeB == null) {
                    break;
                }
                MVCC.Read A = mvcc.read(this, "A");
                MVCC.Read B = mvcc.read(this, "B");
                MVCC.Writehandle writeC = mvcc.intend_to_write(this,"B", A.value + B.value, A.timestamp);

                if (writeC == null) {
                    break;
                }

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
        rts.clear();
        writehandles.clear();
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
}
