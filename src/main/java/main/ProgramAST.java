package main;

import java.util.ArrayList;
import java.util.List;

public class ProgramAST extends AST {
    public int threads;
    private List<AST> children;

    public ProgramAST(String token) {
        this.threads = Integer.parseInt(token);
        this.children = new ArrayList<>();
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("ProgramAST threads=%d\n", threads));
        for (AST child : children) {
            sb.append(String.format("- %s\n", child.toString()));
        }
        return sb.toString();
    }

    @Override
    public void add(AST astNode) {
        this.children.add(astNode);
    }
}
