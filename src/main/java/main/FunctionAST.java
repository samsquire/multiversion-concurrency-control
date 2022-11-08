package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionAST extends AST {
    private final List<Argument> arguments;
    public List<AST> children;
    private String functionName;

    public FunctionAST(String functionName, List<Argument> arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
        this.children = new ArrayList<>();
    }

    @Override
    public void add(AST astNode) {

        this.children.add(astNode);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("function %s\n", functionName));
        for (Argument argument : arguments) {
            sb.append(argument + " \n");
        }
        for (AST child : children) {
            sb.append(String.format("- %s\n", child));
        }
        return sb.toString();
    }

    @Override
    public CodeSegment codegen() {
        List<Map<String, String>> genned = new ArrayList<>();
        List<String> instructions = new ArrayList<>();
        Map<String, String> parsed = new HashMap<String, String>();
        parsed.put("label", functionName);
        instructions.add("createlabel");
        genned.add(parsed);
        for (AST ast : children) {
            System.out.println(String.format("Generating codegen for ast %s", ast));
            CodeSegment codegen = ast.codegen();
            System.out.println(codegen);
            instructions.addAll(codegen.instructions);
            genned.addAll(codegen.parsed);
        }

        return new CodeSegment(instructions, genned);
    }
}
