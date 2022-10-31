package main;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public abstract class AST {

    public List<Map<String, String>> codegen() {
        return asList();
    }

    public abstract void add(AST astNode);
}
