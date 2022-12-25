package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableDeclarationAST extends AST {
    private final String variableType;
    private final String variableName;
    private AST expression;

    public VariableDeclarationAST(String variableType, String variableName, AST expression) {
        this.variableType = variableType;
        this.variableName = variableName;
        this.expression = expression;
    }

    @Override
    public void add(AST astNode) {

    }
    public String toString() {

        return "%s %s = %s".format(variableType, variableName, expression);
    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating type declaration", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();

        instructions.add("define");
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("variable", variableName);
        parsed.put("type", variableType);
        genned.add(parsed);

//        switch (variableType) {
//            case "struct":
//                instructions.add("pushstruct");
//                HashMap<String, String> pushstruct = new HashMap<>();
//                pushstruct.put("variable", variableName);
//                genned.add(pushstruct);
//        }

        CodeSegment codegen = expression.codegen();
        instructions.addAll(codegen.instructions);
        genned.addAll(codegen.parsed);

//        instructions.add("push");
//        HashMap<String, String> push = new HashMap<>();
//        push.put("type", variableType);
//        genned.add(push);

        instructions.add("store");
        HashMap<String, String> store = new HashMap<>();
        store.put("variable", variableName);
        store.put("type", variableType);
        genned.add(store);

//        switch (variableType) {
//            case "struct":
//                instructions.add("popstruct");
//                HashMap popstruct = new HashMap<>();
//                genned.add(popstruct);
//                break;
//        }

        return new CodeSegment(instructions, genned);
    }
}
