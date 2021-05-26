package main;

import main.MVCC;

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
                System.out.println("Was previously aborted");
                MVCC.Writehandle writeA = mvcc.intend_to_write(this,"A", 5);
                if (writeA == null) {
                    break;
                }
                writehandles.add(writeA);
                MVCC.Writehandle writeB = mvcc.intend_to_write(this,"B", 5);
                if (writeB == null) {
                    break;
                }
                writehandles.add(writeB);
                int A = mvcc.read("A", this);
                int b = mvcc.read("B", this);
                MVCC.Writehandle writeC = mvcc.intend_to_write(this,"B", A + b);
                if (writeC == null) {
                    break;
                }
                writehandles.add(writeC);
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
}
