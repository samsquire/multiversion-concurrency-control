package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConcurrentLoopRunner {
    public static void main(String[] args) {
        String[] letters = new String[] { "a", "b", "c", "d"};
        String[] numbers = new String[] { "1", "2", "3", "4"};
        String[] symbols = new String[] { "÷", "×", "(", "&"};

        List<List<String>> collections = new ArrayList<>();
        collections.add(Arrays.asList(letters));
        collections.add(Arrays.asList(numbers));
        collections.add(Arrays.asList(symbols));


        ConcurrentLoop<String> cl = new ConcurrentLoop<>("letters", collections, new ConcurrentLoop.LoopRunner<String>() {
            @Override
            public String run(List<ConcurrentLoop<String>> downstreams, List<String> items) {
                StringBuilder sb = new StringBuilder();
                for (String item : items) {
                    sb.append(item);
                }
                return sb.toString();
            }
        });
        for (int i = 0 ; i < cl.size(); i++) {
            System.out.println(cl.tick());
        }

    }
}
