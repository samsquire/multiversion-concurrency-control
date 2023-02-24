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
        System.out.println(String.format("Generating literal number declaration", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();

        instructions.add("pushint");
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("token", token);
        genned.add(parsed);

        instructions.add("pushstring");
        HashMap<String, String> parsed2 = new HashMap<>();
        parsed2.put("token", String.valueOf(token));
        genned.add(parsed2);

        instructions.add("pushtype");
        Map<String, String> pushtype = new HashMap<>();
        pushtype.put("type", "int");
        genned.add(pushtype);


        return new CodeSegment(instructions, genned);
    }
    public CodeSegment genparameter() {
        System.out.println(String.format("Generating literal number parameter", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();

        instructions.add("pushargument");
        HashMap<String, String> data = new HashMap<>();

        data.put("argument", token);
        genned.add(data);
        return new CodeSegment(instructions, genned);
    }
}
