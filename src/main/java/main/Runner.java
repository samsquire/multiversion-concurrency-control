package main;

import java.util.ArrayList;
import java.util.List;

public class Runner {
    public static void main(String[] args) throws InterruptedException {
        MVCC mvcc = new MVCC();
        mvcc.ensure_keys("A", "B");
        TransactionA transactionA = new TransactionA(mvcc);
        TransactionB transactionB = new TransactionB(mvcc);
        transactionA.start();
        transactionA.join();
        transactionB.start();
        transactionB.join();
        List<TransactionC> transactions = new ArrayList<>();
        for (int i = 0 ; i < 5; i++) {
            TransactionC transactionC = new TransactionC(mvcc);
            transactionC.start();
            transactions.add(transactionC);
        }
        for (TransactionC transaction : transactions) {
            transaction.join();
        }
        System.out.println("Final state");
        mvcc.dump();

    }
}
