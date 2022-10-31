package main;

import java.util.ArrayList;
import java.util.List;

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
}
