package main;

import java.util.ArrayList;

public class ForLoopAST extends AST {

    private final AST expression;
    private final AST postexpression;
    private final ArrayList<AST> children;
    private final AST assignment;

    public ForLoopAST(AST assignment, AST expression, AST postexpression) {

        this.assignment = assignment;
        this.expression = expression;
        this.postexpression = postexpression;
        this.children = new ArrayList<>();
    }

    @Override
    public void add(AST astNode) {
        this.children.add(astNode);
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("for (%s; %s; %s)", assignment, expression, postexpression));
        sb.append("\n");
        for (AST child : children) {
            sb.append(String.format("- %s\n", child));
        }
        return sb.toString();
    }

    public boolean hasOperator() {
        for (AST child : children) {
            if (child instanceof OperatorAST) {
                // need to process in reverse order
                return true;
            }
            if (child.hasOperator()) {
                return true;
            }
        }
        return false;
    }
}
