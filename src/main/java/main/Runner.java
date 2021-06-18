package main;

import java.util.ArrayList;
import java.util.List;

public class Runner {
    public static void main(String[] args) throws InterruptedException {
        MVCC mvcc = new MVCC(true);
        mvcc.ensure_keys("A", "B");
        TransactionA transactionA = new TransactionA(mvcc);
        TransactionB transactionB = new TransactionB(mvcc);
        transactionA.start();
        transactionA.join();
        transactionB.start();
        transactionB.join();
        int A = mvcc.getLatest("A");
        int B = mvcc.getLatest("B");
        List<TransactionC> transactions = new ArrayList<>();
        for (int i = 0 ; i < 100; i++) {
            TransactionC transactionC = new TransactionC(mvcc);
            transactions.add(transactionC);
            transactionC.start();
        }
        for (TransactionC transaction : transactions) {
            transaction.join();
        }
        System.out.println("Final state");
        mvcc.dump();
        System.out.println(A);
        System.out.println(B);
        System.out.println(mvcc.getLatest("A"));

        System.out.println(mvcc.getLatest("B"));

        assert mvcc.getLatest("A") == 110;
        assert mvcc.getLatest("B") == 125;

    }
}
