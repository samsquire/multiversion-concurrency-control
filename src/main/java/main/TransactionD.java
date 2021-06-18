package main;

import java.util.ArrayList;
import java.util.List;

class TransactionD extends Thread implements MVCC.Transaction {

    private final MVCC mvcc;
    private boolean aborted = true;
    private int timestamp;
    public List<MVCC.Writehandle> writehandles;

    public TransactionD(MVCC mvcc) {
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

                int A = mvcc.read(this, "A");
                int B = mvcc.read(this, "B");
                MVCC.Writehandle writeC = mvcc.intend_to_write(this,"A", A + 1);
                writehandles.add(writeC);
                if (writeC == null) {
                    break;
                }

                MVCC.Writehandle writeD = mvcc.intend_to_write(this,"B", B + 1);
                writehandles.add(writeD);
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
        writehandles.clear();
    }
}
