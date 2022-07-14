package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentLoopJoin extends ConcurrentLoop {

    private final HashMap<String, Integer> wait_size;
    private final ConcurrentHashMap<String, List<List<StringOrConcurrentLoop>>> waits_values;
    private final ConcurrentHashMap<String, ConcurrentHashMap<ConcurrentLoop, List<StringOrConcurrentLoop>>> destinations;
    private final List<List<ConcurrentLoop>> wait_lists;
    private final Map<List<ConcurrentLoop>, List<List<StringOrConcurrentLoop>>> wait_contents;
    public ConcurrentLoopJoin(String name, List<List<StringOrConcurrentLoop>> collections, ConcurrentLoop.LoopRunner func) {
        super(name, collections, func);
        this.wait_size = new HashMap<String, Integer>();
        this.waits_values = new ConcurrentHashMap<String, List<List<StringOrConcurrentLoop>>>();
        this.destinations = new ConcurrentHashMap<String, ConcurrentHashMap<ConcurrentLoop, List<StringOrConcurrentLoop>>>();
        this.wait_lists = new ArrayList<>();
        this.wait_contents = new HashMap<>();
    }

    public void wait_for(String name, List<ConcurrentLoop> values) {
        waits_values.put(name, new ArrayList<List<StringOrConcurrentLoop>>(values.size()));
        wait_size.put(name, values.size());
        for (int i = 0; i < values.size(); i++) {
            List<List<StringOrConcurrentLoop>> lists = waits_values.get(name);
            lists.add(i, new ArrayList<StringOrConcurrentLoop>());
            if (!destinations.containsKey(name)) {
                destinations.put(name, new ConcurrentHashMap<>());
            }
            ConcurrentLoop key = values.get(i);
            Map<ConcurrentLoop, List<StringOrConcurrentLoop>> concurrentLoopListMap = destinations.get(name);
            concurrentLoopListMap.put(key, lists.get(i));
        }
        synchronized (wait_lists) {
            wait_lists.add(values);
        }
        wait_contents.put(values, waits_values.get(name));
    }

    public int size() {
        int total = 0;
        synchronized (wait_lists) {
            for (List<ConcurrentLoop> wait_list : wait_lists) {
                List<List<StringOrConcurrentLoop>> all_fields = wait_contents.get(wait_list);
                int desiredSize = wait_list.size();
                int min = Integer.MAX_VALUE;
                for (int i = 0; i < all_fields.size(); i++) {
                    min = Math.min(min, all_fields.get(i).size());
                }
                total += min;

            }
        }

        return total;
    }


    public synchronized void enqueue(int index, StringOrConcurrentLoop item, ConcurrentLoop parent) {
        Map<ConcurrentLoop, List<StringOrConcurrentLoop>> concurrentLoopListMap = destinations.get(parent.name);
        List<StringOrConcurrentLoop> stringOrConcurrentLoops = concurrentLoopListMap.get(parent);
        stringOrConcurrentLoops.add(item);
    }

    @Override
    public StringOrConcurrentLoop tick(int n) {
        if (!super.timesCalled.containsKey(n)) {
            super.timesCalled.put(n, 1);
        } else {
            super.timesCalled.put(n, super.timesCalled.get(n) + 1);
        }


        for (Map.Entry<String, ConcurrentHashMap<ConcurrentLoop, List<StringOrConcurrentLoop>>> item : destinations.entrySet()) {
            List<StringOrConcurrentLoop> items = new ArrayList<>();
            int size = 0;
            int availableSize = 0;

            for (Map.Entry<ConcurrentLoop, List<StringOrConcurrentLoop>> upstream : item.getValue().entrySet()) {
                size = size + upstream.getValue().size();
            }
            for (Map.Entry<ConcurrentLoop, List<StringOrConcurrentLoop>> upstream : item.getValue().entrySet()) {


                if (upstream.getValue().size() > 0) {
                    availableSize++;
                }


            }
            Integer neededSize = wait_size.get(item.getKey());
            if (neededSize == null) {
                break;
            }
            int collected = 0;

            if (size >= neededSize && availableSize >= neededSize) {
                for (Map.Entry<ConcurrentLoop, List<StringOrConcurrentLoop>> upstream : item.getValue().entrySet()) {


                    if (upstream.getValue().size() > 0) {
                        items.add(upstream.getValue().remove(0));
                        collected++;
                    }
                    if (collected == neededSize) {
                        break;
                    }


                }

                return super.func.run(this, super.downstreams, items);
            }
        }
        return null;
    }

}
