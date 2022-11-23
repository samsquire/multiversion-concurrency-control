package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifierAST extends AST {
    public final String token;
    public final String type;


    public IdentifierAST(String token, String type) {
        super();
        this.token = token;
        this.type = type;
    }

    @Override
    public void add(AST astNode) {

    }
    public String toString() {
        return String.format("Identifier %s",  token);
    }

    @Override
    public CodeSegment codegen() {
        System.out.println("Generating identifier AST");
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();
        if (type.equals("string")) {
            instructions.add("load");
        } else {
            instructions.add("pushint");
        }
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("variable", token);
        parsed.put("token", token);
        genned.add(parsed);
        return new CodeSegment(instructions, genned);
    }
}
