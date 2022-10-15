package main;

import java.util.*;

public class CountingNetworks {


    public static void main(String[] args) {

        List<Balancer> root = new ArrayList<>();
        int width = 4;
        int depth = 4;

        PriorityQueue<Balancer> previous = new PriorityQueue<>(new Comparator<Balancer>() {
            @Override
            public int compare(Balancer o1, Balancer o2) {
                return o2.connections - o1.connections;
            }
        });
        int id = 0;
        for (int w = 0; w < depth; w++) {
            Balancer balancer = new Balancer(id++);
            previous.add(balancer);
            root.add(balancer);
        }
        PriorityQueue<Balancer> newPrevious = null;
        for (int w = 0; w < depth; w++) {
            newPrevious = new PriorityQueue<>(new Comparator<Balancer>() {
                @Override
                public int compare(Balancer o1, Balancer o2) {
                    return o2.connections - o1.connections;
                }
            });

            for (int d = 0; d < width; d++) {
                System.out.println(String.format("%d%d", w, d));
                Balancer balancer = new Balancer(id++);
                newPrevious.add(balancer);

                Balancer remove = previous.remove();

                remove.connect(balancer);
                Balancer remove1 = previous.remove();

                remove1.connect(balancer);
                if (remove.connections < 2) {
                    previous.add(remove);
                }
                if (remove1.connections < 2) {
                    previous.add(remove1);
                }

            }
            previous = newPrevious;

        }
//        for (Balancer balancer : root) {
           root.get(0).print("");
//        }
        List<Collector> collectors = new ArrayList<>();
        for (Balancer balancer : newPrevious) {
            Collector collector1 = new Collector(100);
            balancer.connect(collector1);
            Collector collector2 = new Collector(100);
            balancer.connect(collector2);
            collectors.add(collector1);
            collectors.add(collector2);
        }


        int current = 0;
        Random rng = new Random();
        List<Integer> sentTokens = new ArrayList<>();
        for (int i = 100; i >= 0; i--) {
            int token = rng.nextInt(Integer.MAX_VALUE);
            sentTokens.add(token);
            root.get(current).send(token);
            current = (current + 1) % width;
        }
        current = 0;
        int size = 0;
        for (Collector collector : collectors) {
            size += collector.tokens.size();
        }
        List<List<Integer>> receivedTokens = new ArrayList<>();
        for (Collector collector : collectors) {
            List<Integer> pop = collector.pop();
            if (pop != null) {
                receivedTokens.add(pop);
            }
        }
        List<Integer> counted = new ArrayList<>();
        size = 0;
        boolean remaining = true;
        while (remaining) {
            remaining = false;
            for (List<Integer> tokenBatch : receivedTokens) {

                System.out.println(tokenBatch);
                if (tokenBatch.size() > size) {
                    remaining = true;
                    counted.add(tokenBatch.get(size));
                }

            }
            size = size + 1;
        }
        System.out.println(counted);
        Integer previous2 = Integer.MIN_VALUE;
        int index = 0;
        for (Integer count : counted) {
            assert previous2 < count : index;
            previous2 = count;
            index = index + 1;
        }
    }

    private static class Collector extends Balancer {
        public Collector(int id) {
            super(id);
        }
        public List<Integer> tokens = new ArrayList<>();

        public void send(int token) {
            this.tokens.add(token);
        }

        public List<Integer> pop() {
            if (this.tokens.size() > 0) {
               return this.tokens;
            }
            return null;
        }
    }

    private static class Balancer {
        private final int id;
        int connections = 0;
        private List<Balancer> outputs;
        int current = 0;
        int size = 0;
        private List<Integer> signals;
        List<Integer> sizes;

        public Balancer(int id) {
            this.id = id;
            this.outputs = new ArrayList<>();
            this.sizes = new ArrayList<>();
            this.sizes.add(0);
            this.sizes.add(0);
        }

        public void connect(Balancer balancer) {
            this.outputs.add(balancer);
            this.connections++;
            System.out.println(String.format("Connecting %d to %d", id, balancer.id));
        }

        public void print(String indent) {
            for (Balancer balancer : outputs) {
                System.out.println(String.format("%s %s", indent, balancer.id));
                balancer.print(indent + "\t");
            }
        }

        public void send(int token) {
            if (this.sizes.get(0) > this.sizes.get(1)) {
                this.outputs.get(1).send(token);
                this.sizes.set(1, this.sizes.get(1) + 1);
            } else {
                this.outputs.get(0).send(token);
                this.sizes.set(0, this.sizes.get(0) + 1);
            }



        }
    }
}
