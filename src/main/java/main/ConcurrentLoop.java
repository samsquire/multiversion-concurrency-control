package main;

import java.util.*;

public class ConcurrentLoop<T> extends Thread {
    private List<List<T>> collections;
    private final String name;
    private final Map<String, ConcurrentLoop<T>> context;
    private final List<ConcurrentLoop<T>> downstreams;
    private int n;
    private final LoopRunner<T> func;
    private final List<Integer> current;
    private final List<List<T>> pending;

    public interface LoopRunner<T> {
        T run(List<ConcurrentLoop<T>> downstreams, List<T> items);
    }

    public ConcurrentLoop(String name, List<List<T>> collections, LoopRunner<T> func) {
        this.collections = collections;
        this.name = name;
        this.n = 0;
        this.func = func;
        this.context = new HashMap<>();
        this.downstreams = new ArrayList<>();
        this.pending = new ArrayList<>();
        this.current = new ArrayList<Integer>();
        for (List<T> collection : collections) {
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

    public void enqueue(int index, T item, ConcurrentLoop ticker) {
        if (!(this.pending.size() > index)) {
            pending.add(new ArrayList<T>());
        }
        this.pending.get(index).add(item);

    }

    public void link(ConcurrentLoop<T> ticker) {
        this.downstreams.add(ticker);
    }

    public void reload() {
        this.collections = this.pending;

        this.n = 0;
    }

    public T tick() {

        List<T> items = new ArrayList<>();
        int n = this.n;
        List<Integer> indexes = new ArrayList<>();


        for (int i = collections.size() - 1; i >= 0; i--) {
            List<T> collection = collections.get(i);
            int size = collection.size();
            n = n / size;
            int r = n % size;
            indexes.add(r);
        }


        for (int loop = 0; loop < collections.size(); loop++) {
            int previous = 0;
            for (int mod = 0; mod < collections.size(); mod++) {
                previous = previous + indexes.get(mod);
            }


            indexes.set(loop, previous % collections.get(loop).size());
        }

        // for loop, item in enumerate(self.indexes):
        for (int loop = 0; loop < indexes.size(); loop++) {
            items.add(collections.get(loop).get(indexes.get(loop)));
        }

        this.n = this.n + 1;
        return func.run(downstreams, items);

    }


}
