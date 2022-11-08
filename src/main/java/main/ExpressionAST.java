package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionAST extends AST {
    private List<AST> children;

    public ExpressionAST(AST ast) {
        this.children = new ArrayList<>();
        this.children.add(ast);
    }
    @Override
    public void add(AST astNode) {
        this.children.add(astNode);
    }

    @Override
    public CodeSegment codegen() {
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();
        for (AST ast : children) {
            CodeSegment codegen = ast.codegen();
            instructions.addAll(codegen.instructions);
            genned.addAll(codegen.parsed);
        }
        return new CodeSegment(instructions, genned);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (AST child : children) {
            sb.append(String.format("%s ", child));
        }
        sb.append("]");
        return sb.toString();
    }
}
