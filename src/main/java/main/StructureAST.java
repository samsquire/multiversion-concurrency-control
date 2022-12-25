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

        instructions.add("pushstruct");
        HashMap<String, String> struct = new HashMap<>();
        genned.add(struct);

        for (Map.Entry<AST, AST> entry : ast.entrySet()) {
            CodeSegment codegen = entry.getKey().codegen();
            System.out.println(String.format("KEY CODEGEN %s", codegen.instructions));
            instructions.addAll(codegen.instructions);
            genned.addAll(codegen.parsed);
            instructions.add("pushkey");

            Map<String, String> pushkey = new HashMap<>();
            pushkey.put("type", "struct"); // TODO: use entry.getKey() key type
            genned.add(pushkey);
            CodeSegment valuecodegen = entry.getValue().codegen();
            instructions.addAll(valuecodegen.instructions);
            genned.addAll(valuecodegen.parsed);
            instructions.add("pushvalue");
            Map<String, String> pushvalue = new HashMap<>();
            pushvalue.put("type", "string"); // TODO: determine type of expression
            genned.add(pushvalue);



            instructions.add("poptype");
            genned.add(new HashMap<>());

//            if ((entry.getValue()).getClass() == StructureAST.class) {
//                System.out.println("popping structure");
//                instructions.add("popstruct");
//                Map<String, String> popstruct = new HashMap<>();
//                genned.add(popstruct);
//            }

        }

        instructions.add("pushtype");
        Map<String, String> pushtype = new HashMap<>();
        pushtype.put("type", "struct");
        genned.add(pushtype);


//        System.out.println("popping structure");
//        instructions.add("popstruct");
//        Map<String, String> popstruct = new HashMap<>();
//        genned.add(popstruct);


        return new CodeSegment(instructions, genned);
    }
}
