package main;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProgramAST extends AST {
    public int threads;
    private List<AST> children;

    @Override
    public CodeSegment codegen() {
        List<Map<String, String>> genned = new ArrayList<>();
        List<String> instructions = new ArrayList<>();
        for (AST ast : children) {
            System.out.println(String.format("Generating codegen for %s", ast));
            CodeSegment codegen = ast.codegen();
            genned.addAll(codegen.parsed);
            instructions.addAll(codegen.instructions);
        }
        return new CodeSegment(instructions, genned);
    }

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
