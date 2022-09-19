package main;

import java.util.*;

public class ConcurrentLoop {
    private List<List<StringOrConcurrentLoop>> collections;
    public final String name;
    protected final Map<String, ConcurrentLoop> context;
    protected final List<ConcurrentLoop> downstreams;
    protected final LoopRunner func;
    private final List<Integer> current;
    private final List<List<StringOrConcurrentLoop>> pending;
    public Map<Integer, Integer> timesCalled = new HashMap<>();
    public Map<String, StringOrConcurrentLoop> reuse;
    public List<Integer> threads = new ArrayList<>();


    public void registerChain(String parentName, StringOrConcurrentLoop stringOrConcurrentLoop) {
        reuse.put(parentName, stringOrConcurrentLoop);
    }
    public boolean isReuse(String parentName) {
        return reuse.containsKey(parentName);
    }

    public StringOrConcurrentLoop reuse(String parentName) {
        return reuse.get(parentName);
    }


    public interface LoopRunner {
        StringOrConcurrentLoop run(ConcurrentLoop parent,
              List<ConcurrentLoop> downstreams,
              List<StringOrConcurrentLoop> items);
    }

    public ConcurrentLoop(String name, List<List<StringOrConcurrentLoop>> collections, LoopRunner func) {
        this.collections = collections;
        this.name = name;
        this.func = func;
        this.context = new HashMap<>();
        this.downstreams = new ArrayList<>();
        this.pending = new ArrayList<>();
        this.current = new ArrayList<Integer>();
        this.reuse = new HashMap<>();
        for (List<StringOrConcurrentLoop> collection : collections) {
            this.current.add(0);
        }
    }

    public int size() {
        if (this.collections.size() == 0) {
            return 0;
        }
        int total = this.collections.get(0).size();
        for (int i = 1; i < this.collections.size(); i++) {
            total = total * this.collections.get(i).size();
        }
        return total;
    }

    public void enqueue(int index, StringOrConcurrentLoop item, ConcurrentLoop ticker) {
        if (!(this.pending.size() > index)) {
            pending.add(new ArrayList<StringOrConcurrentLoop>());
        }
        this.pending.get(index).add(item);

    }

    public void link(ConcurrentLoop ticker) {
        this.downstreams.add(ticker);
    }

    public void reload() {
        this.collections = this.pending;
    }

    public StringOrConcurrentLoop tick(int n) {
        List<List<StringOrConcurrentLoop>> collections = this.collections;
        if (collections.size() == 0) {
            return new StringOrConcurrentLoop(null, null);
        }
        if (!timesCalled.containsKey(n)) {
            timesCalled.put(n, 1);
        } else {
            timesCalled.put(n, timesCalled.get(n) + 1);
        }


        List<StringOrConcurrentLoop> items = new ArrayList<>();

        List<Integer> indexes = new ArrayList<>();


        for (int i = collections.size() - 1; i >= 0; i--) {
            List<StringOrConcurrentLoop> collection = collections.get(i);
            int size = collection.size();
            int newN = n / size;
            int r = n % size;
            n = newN;
            indexes.add(r);
        }



        for (int loop = 0; loop < collections.size(); loop++) {
            int previous = 0;
            for (int mod = 0; mod < collections.size(); mod++) {
                previous = previous + indexes.get(mod);
            }
            indexes.set(loop, previous % collections.get(loop).size());
        }

        // create string item
        for (int loop = 0; loop < indexes.size(); loop++) {
            items.add(collections.get(loop).get(indexes.get(loop)));
        }
        Collections.reverse(items);

        return func.run(this, downstreams, items);

    }

    public void addThread(int threadNum) {
        this.threads.add(threadNum);
    }

}
