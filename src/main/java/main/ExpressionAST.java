package main;

import java.util.ArrayList;
import java.util.List;

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
