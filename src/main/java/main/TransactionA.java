package main;

import java.util.ArrayList;
import java.util.List;

class TransactionA extends Thread implements MVCC.Transaction {

    private final MVCC mvcc;
    private boolean aborted = true;
    private int timestamp;
    public List<MVCC.Writehandle> writehandles;

    public TransactionA(MVCC mvcc) {
        this.mvcc = mvcc;
        this.writehandles = new ArrayList<>();

    }

    @Override
    public void run() {
        super.run();
        while (aborted) {
            while (true) {
                timestamp = mvcc.issue();

                System.out.println(String.format("Was previously aborted %d", timestamp));
                MVCC.Writehandle writeA = mvcc.intend_to_write(this,"A", 5);

                if (writeA == null) {
                    break;
                }
                MVCC.Writehandle writeB = mvcc.intend_to_write(this,"B", 5);

                if (writeB == null) {
                    break;
                }
                int A = mvcc.read(this, "A");
                int B = mvcc.read(this, "B");
                MVCC.Writehandle writeC = mvcc.intend_to_write(this,"B", A + B);

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
        writehandles.clear();
    }
    @Override
    public void addWrite(MVCC.Writehandle writehandle) {
        writehandles.add(writehandle);
    }
}
