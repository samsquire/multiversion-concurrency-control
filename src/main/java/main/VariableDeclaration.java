package main;

import java.util.Map;

public class VariableDeclaration {
    public String name;
    public String type;
    public Map<String, String> parsed;

    public VariableDeclaration(String name, Map<String, String> parsed, String type) {
        this.name = name;
        this.parsed = parsed;
        this.type = type;
    }
}
