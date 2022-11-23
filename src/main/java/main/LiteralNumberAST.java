package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiteralNumberAST extends AST {
    private String token;

    public LiteralNumberAST(String token) {
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

        instructions.add("pushint");
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("token", token);
        genned.add(parsed);

        return new CodeSegment(instructions, genned);
    }
}
