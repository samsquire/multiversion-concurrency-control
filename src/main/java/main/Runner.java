package main;

public class Runner {
    public static void main(String[] args) {
        MVCC mvcc = new MVCC();
        TransactionA transactionA = new TransactionA(mvcc);
        TransactionB transactionB = new TransactionB(mvcc);
        transactionA.start();
        transactionB.start();
    }
}
