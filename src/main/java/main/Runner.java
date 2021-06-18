package main;

public class Runner {
    public static void main(String[] args) throws InterruptedException {
        MVCC mvcc = new MVCC();
        mvcc.ensure_keys("A", "B");
        TransactionA transactionA = new TransactionA(mvcc);
        TransactionB transactionB = new TransactionB(mvcc);
        transactionB.start();
        transactionA.start();
        transactionA.join();
        transactionB.join();
        TransactionC transactionC = new TransactionC(mvcc);
        TransactionD transactionD = new TransactionD(mvcc);
        transactionC.start();
        transactionD.start();
    }
}
