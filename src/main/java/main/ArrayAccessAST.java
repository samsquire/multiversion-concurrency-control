package main;

import java.util.ArrayList;

public class ArrayAccessAST extends AST {
    private final ArrayList<AST> children;

    public ArrayAccessAST() {
        this.children = new ArrayList<>();
    }
    @Override
    public void add(AST astNode) {
        this.children.add(astNode);
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("arrayaccess");
        for (AST child : children) {
            sb.append(String.format("[%s]", child));
        }
        return sb.toString();
    }
}
