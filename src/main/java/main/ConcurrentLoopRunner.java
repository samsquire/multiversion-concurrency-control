package main;

import java.util.*;

public class ConcurrentLoopRunner {
    public static void main(String[] args) throws InterruptedException {
        String[] letters = new String[] { "a", "b", "c", "d"};
        String[] numbers = new String[] { "1", "2", "3", "4"};
        String[] symbols = new String[] { "÷", "×", "(", "&"};
        String[] symbol1 = new String[] {":", "<", ">"};
        String[] symbol2 = new String[] {"*", "!", "#"};

        List<List<StringOrConcurrentLoop>> collections = new ArrayList<>();
        List<StringOrConcurrentLoop> letterlist = new ArrayList<>();
        for (String letter : letters) {
            letterlist.add(new StringOrConcurrentLoop(letter, null));
        }
        List<StringOrConcurrentLoop> numberlist = new ArrayList<>();
        for (String number : numbers) {
            numberlist.add(new StringOrConcurrentLoop(number, null));
        }
        List<StringOrConcurrentLoop> symbollist = new ArrayList<>();
        for (String symbol : symbols) {
            symbollist.add(new StringOrConcurrentLoop(symbol, null));
        }
        collections.add(letterlist);
        collections.add(numberlist);
        collections.add(symbollist);

        List<StringOrConcurrentLoop> symbol1list = new ArrayList<>();
        for (String symbol : symbol1) {
            symbol1list.add(new StringOrConcurrentLoop(symbol, null));
        }
        List<StringOrConcurrentLoop> symbol2list = new ArrayList<>();
        for (String symbol : symbol2) {
            symbol2list.add(new StringOrConcurrentLoop(symbol, null));
        }

        ConcurrentLoop.LoopRunner communicator = new ConcurrentLoop.LoopRunner() {
            public StringOrConcurrentLoop run(ConcurrentLoop parent,
                                                      List<ConcurrentLoop> downstreams,
                                                      List<StringOrConcurrentLoop> items) {
                StringBuilder sb = new StringBuilder();

                for (StringOrConcurrentLoop item : items) {
                    sb.append(item.value);
                }
                System.out.println(String.format("ENQUEUE %s%s", parent.name, sb.toString()));
                for (ConcurrentLoop downstream : downstreams) {

                    downstream.enqueue(0, new StringOrConcurrentLoop(sb.toString(), null), parent);
                }
                return new StringOrConcurrentLoop(null, null);
            }
        };

        ConcurrentLoop downstream = new ConcurrentLoopJoin("agg", new ArrayList<>(), new ConcurrentLoop.LoopRunner() {
            @Override
            public StringOrConcurrentLoop run(ConcurrentLoop parent, List<ConcurrentLoop> downstreams, List<StringOrConcurrentLoop> items) {
                StringBuilder sb = new StringBuilder();
                for (StringOrConcurrentLoop item : items) {
                    if (item.isString()) {
                        sb.append(item.value);
                    }

                }
                return new StringOrConcurrentLoop(sb.toString(), null);
            }
        });



        ConcurrentLoop cl = new ConcurrentLoop("letters", collections, new ConcurrentLoop.LoopRunner() {
            @Override
            public StringOrConcurrentLoop run(ConcurrentLoop parent, List<ConcurrentLoop> downstreams, List<StringOrConcurrentLoop> items) {
                List<List<StringOrConcurrentLoop>> alist = new ArrayList<>();
                alist.add(symbol1list);
                alist.add(items);
                List<List<StringOrConcurrentLoop>> blist = new ArrayList<>();
                blist.add(symbol2list);
                blist.add(items);


                StringBuilder sb = new StringBuilder();

                for (int i = items.size() - 1 ; i >= 0 ; i--) {
                    if (items.get(i).isString()) {
                        sb.append(items.get(i).value);
                    }

                }
                String parentName = sb.toString();
                if (parent.isReuse(parentName)) {
                    return null;
                }

                ConcurrentLoop one = new ConcurrentLoop(parentName, alist, communicator);



                one.link(parent.context.get("d"));
                ConcurrentLoop two = new ConcurrentLoop(parentName, blist, communicator);
                two.link(parent.context.get("d"));
                ((ConcurrentLoopJoin )parent.context.get("d")).wait_for(parentName, Arrays.asList(one, two));
                StringOrConcurrentLoop reuse = new StringOrConcurrentLoop(null, Arrays.asList(one, two));
                parent.registerChain(parentName, reuse);
                return reuse;
            }
        });
        cl.context.put("d", downstream);
        List<ConcurrentLoopThread> threads = new ArrayList<>();
        List<ConcurrentLoop> tickers = Collections.synchronizedList(new ArrayList<>());
        tickers.add(cl);
        tickers.add(downstream);
        System.out.println(cl.size());
        int size = cl.size() / 8;
        for (int i = 0, threadNum = 0 ; i < cl.size(); threadNum++, i += size) {
            ConcurrentLoopThread concurrentLoopThread = new ConcurrentLoopThread(tickers, i, size, threadNum);
            threads.add(concurrentLoopThread);
            concurrentLoopThread.start();
        }
        for (ConcurrentLoopThread thread : threads) {
            thread.join();
        }
        for (ConcurrentLoop cl2 : tickers) {
            for (Map.Entry<Integer, Integer> entry : cl2.timesCalled.entrySet()) {
                assert entry.getValue() == 1;
            }
        }
    }
}
