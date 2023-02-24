package main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SetKeyHash {

    public static void main(String args[]) {
        HashSet<String> vegetables = new HashSet<>();
        vegetables.add("tomato");
        vegetables.add("carrot");
        vegetables.add("broccoli");
        HashSet<String> fruit = new HashSet<>();
        fruit.add("apple");
        fruit.add("kiwi");
        fruit.add("orange");

        Map<HashSet<String>, String> map = new HashMap<>();
        map.put(vegetables, "vegetables");
        map.put(fruit, "fruit");

        HashSet<String> search = new HashSet<>();
        search.add("tomato");
        search.add("carrot");
        search.add("broccoli");
        System.out.println("Exact set search");
        System.out.println(map.get(search));
        System.out.println("Partial set search");
        HashSet<String> partialSearch = new HashSet<>();
        partialSearch.add("tomato");
        partialSearch.add("broccoli");
        for (HashSet<String> set : map.keySet()) {
            if (set.containsAll(partialSearch)) {
                System.out.println(String.format("Found partial match %s", map.get(set)));
            }
        }

    }
}
