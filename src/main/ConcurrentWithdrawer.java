package main;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConcurrentWithdrawer {

    private Map<String, Integer> database = new HashMap<>();
    private int transactionCount = 0;
    private final List<Transaction> transactions = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try {
            new ConcurrentWithdrawer().run();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public void run() throws ExecutionException, InterruptedException {
        int startAmount = 200;
        int numberAccounts = 5;
        int totalMoney = 0;
        for (int i = 0; i < numberAccounts; i++) {
            database.put(String.format("account%d", i), startAmount);
            totalMoney += startAmount;
        }


        ThreadPoolExecutor executor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

        List<Future> futures = new ArrayList<Future>();
        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() {

                    for (int j = 0; j < 5; j++) {

                        Transaction transaction = beginTransaction(transactions, database);

                        transaction.read("fromBalance", "fromAccountName", (context) -> {
                            int fromAccount = getRandomNumberInRange(0, 4);
                            String fromAccountName = String.format("account%d", fromAccount);
                            return fromAccountName;
                        }).read("toBalance", "toAccountName", (context) -> {
                            int toAccount = getRandomNumberInRange(0, 4);
                            String toAccountName = String.format("account%d", toAccount);
                            while (toAccountName.equals(context.lookupName("fromAccountName"))) {
                                toAccount = getRandomNumberInRange(0, 4);
                                toAccountName = String.format("account%d", toAccount);
                            }
                            return toAccountName;
                        }).write("fromAccountName", (writeContext) -> {
                            int difference;
                            TransactionContext context = writeContext.context;
                            if (context.get("fromBalance") >= 100) {
                                difference = 100;
                            } else {
                                difference = 0;
                            }
                            context.write(writeContext.writeStep, "fromAccountName", context.get("fromBalance") - difference);
                            context.put("difference", difference);
                        }).write("toAccountName", (writeContext) -> {
                            TransactionContext context = writeContext.context;
                            context.write(writeContext.writeStep, "toAccountName", context.get("toBalance") + context.get("difference"));
                        }).commit();

                    }

                    int foundMoney = 0;
                    for (int j = 0; j < numberAccounts; j++) {
                        Integer foundMoney1;
                        String account = String.format("account%d", j);
                        foundMoney1 = database.get(account);

                        foundMoney += foundMoney1;
                    }

                    return foundMoney;
                }
            }));
        }

        List<Integer> monies = new ArrayList<>();
        for (Future f : futures) {
            int foundMoney = (Integer) f.get();
            monies.add(foundMoney);
        }
        System.out.println("Totals while running");
        for (Integer money : monies) {
            System.out.println(money);
        }
        System.out.println("Expected money");
        System.out.println(totalMoney);
        System.out.println("Final money");
        int foundMoney = 0;
        for (int j = 0; j < numberAccounts; j++) {
            Integer foundMoney1;

            foundMoney1 = database.get(String.format("account%d", j));
            System.out.println(String.format(String.format("account%d %d", j, foundMoney1)));
            foundMoney += foundMoney1;
        }
        System.out.println(foundMoney);
        executor.shutdown();
    }

    private Transaction beginTransaction(List<Transaction> transactions, Map<String, Integer> database) {
        transactionCount = transactionCount + 1;
        Transaction transaction = new Transaction(transactions, transactionCount, database);
        this.transactions.add(transaction);
        return transaction;
    }

    private class Transaction {
        public Long readTimestamp = 0L;
        public Long writeTimestamp = 0L;
        public List<String> readTargets = new ArrayList<>();
        private List<Transaction> transactions;
        private final int id;
        private Map<String, Integer> database;
        private List<TransactionStep> steps = new ArrayList<>();
        private TransactionContext transactionContext = new TransactionContext();
        private boolean active = true;
        private boolean cancel = false;
        private long transactionFinish;
        private long transactionStart;
        private int reread;
        private boolean valid;

        public Transaction(List<Transaction> transactions, int id, Map<String, Integer> database) {
            this.transactions = transactions;
            this.id = id;
            this.database = database;
        }


        public Transaction read(String field, String name, Function<TransactionContext, String> keyGetter) {
            ReadStep step = new ReadStep(this, field, keyGetter);
            steps.add(step);
            transactionContext.registerStep(name, step);
            return this;
        }

        public Transaction write(String fieldName, Consumer<WriteContext> writer) {
            steps.add(new WriteStep(this, fieldName, writer));
            return this;
        }

        public boolean invalid() {
            long largestWrite = 0L;
            long largestRead = 0L;
            List<Transaction> cloned = new ArrayList<>(transactions);
            cloned.sort(new Comparator<Transaction>() {
                @Override
                public int compare(Transaction o1, Transaction o2) {
                    return (int) (o1.transactionStart - o2.transactionStart);
                }
            });

            for (Transaction transaction : cloned) {
                ArrayList<TransactionStep> clonedSteps = new ArrayList<>(transaction.steps);
                for (TransactionStep step : clonedSteps) {
                    for (TransactionStep thisStep : steps) {
                        if (step instanceof ReadStep && thisStep instanceof ReadStep) {
                            ReadStep thisReadStep = (ReadStep) thisStep;
                            ReadStep readStep = (ReadStep) step;
                            if (thisReadStep.key.equals(readStep.key)) {
                                if (thisReadStep.timestamp > readStep.timestamp) {
                                    return true;
                                }
                            }
                        }
                        if (step instanceof WriteStep && thisStep instanceof WriteStep) {
                            WriteStep thisWriteStep = (WriteStep) thisStep;
                            WriteStep writeStep = (WriteStep) step;
                            if (thisWriteStep.timestamp > writeStep.timestamp) {
                                return true;
                            }
                        }
                    }
                }

            }

            return false;
        }

        public void commit() {
            boolean needsRunning = true;
            int retryCount = 0;
            transactionStart = System.nanoTime();


                while (needsRunning || invalid()) {
                    readTimestamp = 0L;
                    writeTimestamp = 0L;
                    readTargets.clear();
                    retryCount++;
                    active = true;

                    for (TransactionStep step : steps) {
                        step.run(transactionContext);
                    }

                    needsRunning = false;
                    if (cancel) {
                        needsRunning = true;
                        cancel = false;
                    }
                }



            System.out.println(String.format("Retry count was %d", retryCount));


            for (TransactionStep step : steps) {
                if (step instanceof ReadStep) {
                    String key = ((ReadStep) step).key;
                    Integer value = transactionContext.context.get(key);
                    database.put(key, value);
                }
            }


            transactions.remove(this);
            transactionFinish = System.nanoTime();

        }
    }

    private interface TransactionStep {
        TransactionContext run(TransactionContext context);
    }

    private class ReadStep implements TransactionStep {
        private final String field;
        private final Function<TransactionContext, String> keyGetter;
        private boolean activated;
        private String key;
        public long timestamp;
        Transaction transaction;

        public ReadStep(Transaction transaction, String field, Function keyGetter) {
            this.transaction = transaction;
            this.field = field;
            this.keyGetter = keyGetter;
            this.activated = false;
        }

        public TransactionContext run(TransactionContext context) {
            if (!activated) {
                key = (String) this.keyGetter.apply(context);
            }
            activated = true;
            timestamp = System.nanoTime();
            context.put(field, database.get(key));
            if (transaction.readTimestamp == 0L) {
                transaction.readTimestamp = timestamp;
            }
            transaction.readTargets.add(key);


            return context;
        }
    }

    private class TransactionContext {
        public final HashMap<String, Integer> context;
        private Map<String, ReadStep> readSteps = new HashMap<>();

        public TransactionContext() {
            this.context = new HashMap<>();
        }

        public void registerStep(String name, ReadStep readStep) {
            readSteps.put(name, readStep);
        }

        public void put(String field, Integer integer) {
            this.context.put(field, integer);
        }

        public String lookupName(String name) {
            return readSteps.get(name).key;
        }

        public void write(WriteStep writeStep, String name, Integer newValue) {
            String key = lookupName(name);
            writeStep.key = key;
            context.put(key, newValue);
        }

        public Integer get(String field) {
            return this.context.get(field);
        }
    }

    private class WriteStep implements TransactionStep {
        public String key;
        private boolean activated;
        private String fieldName;
        private final Consumer<WriteContext> writer;
        public long timestamp;
        Transaction transaction;

        public WriteStep(Transaction transaction, String fieldName, Consumer<WriteContext> writer) {
            this.transaction = transaction;
            this.fieldName = fieldName;
            this.writer = writer;
            activated = false;
        }

        @Override
        public TransactionContext run(TransactionContext context) {
            timestamp = System.nanoTime();
            transaction.writeTimestamp = timestamp;
            writer.accept(new WriteContext(this, context));
            activated = true;
            return context;
        }
    }

    private class WriteContext {
        private final WriteStep writeStep;
        private final TransactionContext context;

        public WriteContext(WriteStep writeStep, TransactionContext context) {
            this.writeStep = writeStep;
            this.context = context;
        }
    }
}
