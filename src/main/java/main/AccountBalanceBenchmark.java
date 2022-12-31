package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AccountBalanceBenchmark {
    public static void main(String[] args) {
        Random rng = new Random();

        int size = 1000;
        List<Account> accounts = new ArrayList<>(size);

        for (int j = 0; j < size; j++) {
            accounts.add(new Account(1 + rng.nextInt(1500)));
        }


    }

    private static class Account {
        private int balance;

        public Account(int balance) {
            this.balance = balance;
        }

    }
}
