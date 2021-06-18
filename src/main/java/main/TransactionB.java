package main;

import java.util.ArrayList;
import java.util.List;

class TransactionB extends Thread implements MVCC.Transaction {

    private final MVCC mvcc;
    private boolean aborted = true;
    private int timestamp;
    public List<MVCC.Writehandle> writehandles;

    @Override
    public List<MVCC.Writehandle> getWritehandles() {
        return writehandles;
    }

    public TransactionB(MVCC mvcc) {
        this.mvcc = mvcc;
        this.writehandles = new ArrayList<MVCC.Writehandle>();

    }

    @Override
    public void run() {
        super.run();
        while (aborted) {
            while (true) {
                timestamp = mvcc.issue();
                System.out.println(String.format("Was previously aborted %d", timestamp));
                MVCC.Writehandle writeA = mvcc.intend_to_write(this,"A", 10);

                if (writeA == null) {
                    break;
                }
                MVCC.Writehandle writeB = mvcc.intend_to_write(this,"B", 15);

                if (writeB == null) {
                    break;
                }
                int A = mvcc.read(this, "A");
                int b = mvcc.read(this, "B");
                MVCC.Writehandle writeC = mvcc.intend_to_write(this,"B", A + b);

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
