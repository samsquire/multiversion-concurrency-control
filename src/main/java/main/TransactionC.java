package main;

import main.MVCC;

import java.util.ArrayList;
import java.util.List;

class TransactionC extends Thread implements MVCC.Transaction {

    private final MVCC mvcc;
    private boolean aborted = true;
    private int timestamp;
    public List<MVCC.Writehandle> writehandles;

    public TransactionC(MVCC mvcc) {
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

                int A = mvcc.read("A", this);
                int B = mvcc.read("B", this);
                MVCC.Writehandle writeC = mvcc.intend_to_write(this,"A", A + 1);
                if (writeC == null) {
                    break;
                }
                writehandles.add(writeC);

                MVCC.Writehandle writeD = mvcc.intend_to_write(this,"B", B + 1);
                if (writeD == null) {
                    break;
                }
                writehandles.add(writeC);
                System.out.println(String.format("%d committing", timestamp));
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
