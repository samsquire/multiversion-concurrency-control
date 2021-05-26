package main;

public class Runner {
    public static void main(String[] args) {
        MVCC mvcc = new MVCC();
        mvcc.ensure_keys("A", "B");
        TransactionA transactionA = new TransactionA(mvcc);
        TransactionB transactionB = new TransactionB(mvcc);
        transactionB.start();
        transactionA.start();
    }
}
