package main;

import java.util.ArrayList;
import java.util.List;

public class MegaTreeRunner {

    public static void main(String[] args) throws InterruptedException {
        MegaTree<Integer> megatree = new MegaTree(100, 0, null, false);
        megatree.ensure_keys("a");

        MegaTreeTransaction megaTreeTransaction = new MegaTreeTransaction(megatree, 0, 100);
        megatree.put(0, megaTreeTransaction, "a", 0, 0);
        megatree.commit(0, megaTreeTransaction);
        megatree.dump(0);
        // System.exit(0);
        List<MegaTreeTransaction<Integer>> threads = new ArrayList<>();
        for (int i = 0 ; i < 100; i++) {
            megaTreeTransaction = new MegaTreeTransaction(megatree, i, 100);
            threads.add(megaTreeTransaction);
            megaTreeTransaction.start();
        }
        for (int i = 0 ; i < 100; i++) {
            threads.get(i).join();
        }
        megatree.printDuplicates(0,"a");

        Integer a = megatree.getLatest(0, "a");
        System.out.println(String.format("%d latest", a));
        assert a == 100;

        assert(megatree.versionsInOrder(0,"a"));
        System.out.println(megatree.getHighestVersion(0, "a"));

    }
}
