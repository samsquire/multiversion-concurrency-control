package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParameterListAST extends AST {
    private final ArrayList<AST> children;

    public ParameterListAST() {
        this.children = new ArrayList<>();
    }
    @Override
    public void add(AST astNode) {
        this.children.add(astNode);
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("parameterlist");
        for (AST child : children) {
            sb.append(String.format("[%s]", child));
        }
        return sb.toString();
    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating parameter list AST %s", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();
        System.out.println(this);
        for (AST child : children) {
            CodeSegment cs = child.genparameter();
            instructions.addAll(cs.instructions);
            genned.addAll(cs.parsed);
        }

        return new CodeSegment(instructions, genned);
    }
}
