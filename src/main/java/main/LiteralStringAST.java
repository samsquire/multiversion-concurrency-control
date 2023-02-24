package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiteralStringAST extends AST {
    public String token;

    public LiteralStringAST(String token) {
        super();
        this.token = token;
    }

    @Override
    public void add(AST astNode) {

    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating literal string declaration", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();

        instructions.add("pushstring");
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("token", token);
        genned.add(parsed);

        instructions.add("pushtype");
        Map<String, String> pushtype = new HashMap<>();
        pushtype.put("type", "string");
        genned.add(pushtype);

        return new CodeSegment(instructions, genned);
    }

    @Override
    protected CodeSegment genparameter() {
        System.out.println(String.format("Generating literal string parameter", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();

        instructions.add("pushargumentstr");
        HashMap<String, String> data = new HashMap<>();

        data.put("argument", token);
        genned.add(data);
        return new CodeSegment(instructions, genned);
    }

    @Override
    public String toString() {
        return token;
    }
}
