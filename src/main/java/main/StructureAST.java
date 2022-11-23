package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructureAST extends AST {
    private final Map<AST, AST> ast;

    public StructureAST(Map<AST, AST> data) {
        super();
        this.ast = data;
    }

    @Override
    public void add(AST astNode) {

    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HASH");
        for (Map.Entry<AST, AST> entry : ast.entrySet()) {
            sb.append(String.format("ENTRY %s = %s", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating structure declaration", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();

        for (Map.Entry<AST, AST> entry : ast.entrySet()) {
            CodeSegment codegen = entry.getKey().codegen();
            instructions.addAll(codegen.instructions);
            genned.addAll(codegen.parsed);
            instructions.add("pushkey");

            Map<String, String> pushkey = new HashMap<>();
            pushkey.put("type", "string"); // TODO: use entry.getKey() key type
            genned.add(pushkey);
            CodeSegment valuecodegen = entry.getValue().codegen();
            instructions.addAll(valuecodegen.instructions);
            genned.addAll(valuecodegen.parsed);
            instructions.add("pushvalue");
            Map<String, String> pushvalue = new HashMap<>();
            pushvalue.put("type", "string"); // TODO: determine type of expression
            genned.add(pushvalue);
        }

        return new CodeSegment(instructions, genned);
    }
}
