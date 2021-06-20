package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class TransactionC extends Thread implements MVCC.Transaction {

    private final MVCC mvcc;
    private final ConcurrentHashMap<String, Integer> rts;
    private boolean aborted = true;
    private int timestamp;
    public List<MVCC.Writehandle> writehandles;

    public TransactionC(MVCC mvcc) {
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

}
