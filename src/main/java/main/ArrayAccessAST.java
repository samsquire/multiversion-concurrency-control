package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        sb.append("[ arrayaccess");
        for (AST child : this.children) {
            sb.append(String.format("[%s]", child));
        }
        sb.append("]arrayaccessend");
        return sb.toString();
    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating array access AST %s", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();
        System.out.println("AST arrayaccess children");
        System.out.println(this.children);
        ExpressionAST ast = (ExpressionAST) this.children.get(0);
        IdentifierAST identifier = (IdentifierAST) ast.children.get(0);

        instructions.add("push");
        instructions.add("loadhash");
        HashMap<String, String> addParsed = new HashMap<>();
        addParsed.put("variable", identifier.token);
        addParsed.put("type", identifier.type);
        genned.add(addParsed);
        HashMap<String, String> getStackParsed = new HashMap<>();
        addParsed.put("variable", identifier.token);
        genned.add(getStackParsed);
        return new CodeSegment(instructions, genned);
    }
}
