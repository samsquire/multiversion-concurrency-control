package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiplexedAST {
    public final List<MultiplexingProgramParser.Stateline> statelines;
    public Map<String, List<Pair>> children = new HashMap<>();
    public Map<String, List<Pair>> variables = new HashMap<>();

    public MultiplexedAST(List<MultiplexingProgramParser.Stateline> statelines) {
        this.statelines = statelines;
    }

    public void register(MultiplexingProgramParser.Fact fact, MultiplexingProgramParser.Identifier identifier,
                         MultiplexingProgramParser.Stateline stateline) {
        if (!this.children.containsKey(identifier.identifier)) {
            this.children.put(identifier.identifier, new ArrayList<>());
            for (MultiplexingProgramParser.Fact currentFact : identifier.arguments) {
                System.out.println(String.format("CURRENTFACT %s", currentFact));
                if (!this.variables.containsKey(currentFact.name)) {
                    this.variables.put(currentFact.name, new ArrayList<>());
                }
                for (String variable : currentFact.arguments) {
                    System.out.println(String.format("Creating variable %s", variable));

                }
            }
        }
        Pair pair = new Pair(identifier, stateline, fact);
        this.children.get(identifier.identifier).add(pair);
        for (MultiplexingProgramParser.Fact currentFact : identifier.arguments) {
            if (this.variables.containsKey(currentFact.name)) {
                this.variables.get(currentFact.name).add(pair);
            }
            for (String variable : currentFact.arguments) {

            }
        }

    }

    public MultiplexingProgramParser.Stateline find(Match match) {
        for (Map.Entry<String, List<Pair>> item : children.entrySet()) {
            System.out.println(item.getKey());
            if (item.getKey().equals(match.identifier)) {
                System.out.println(item.getValue());
                for (Pair pair : item.getValue()) {
                    String name = pair.fact.name;
                    System.out.println("fact " + name);
                    System.out.println(match.value);
                    if (name.equals(match.value)) {
                        return pair.stateline;
                    }
                }

            }
        }
        return null;
    }

    public class Pair {
        public final MultiplexingProgramParser.Identifier identifier;
        public final MultiplexingProgramParser.Stateline stateline;
        public final MultiplexingProgramParser.Fact fact;

        public Pair(MultiplexingProgramParser.Identifier identifier,
                    MultiplexingProgramParser.Stateline stateline,
                    MultiplexingProgramParser.Fact fact) {
            this.identifier = identifier;
            this.fact = fact;
            this.stateline = stateline;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(identifier);
            sb.append("\n");
            sb.append(fact);
            sb.append("\n");
            sb.append(stateline);
            sb.append("\n");
            return sb.toString();

        }
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.children.toString());
        return sb.toString();
    }
}
