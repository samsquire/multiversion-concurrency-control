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
        String token = "INVALID";
        AST name = ast.children.get(0);
        if (name.getClass() == LiteralStringAST.class) {

            token = ((LiteralStringAST) name).token;
        }
        if (name.getClass() == IdentifierAST.class) {
            token = ((IdentifierAST) name).token;
        }
        instructions.add("push");
        instructions.add("loadhash");
        HashMap<String, String> addParsed = new HashMap<>();
        addParsed.put("variable", token);
        addParsed.put("type", "string");
        genned.add(addParsed);
        HashMap<String, String> getStackParsed = new HashMap<>();
        addParsed.put("variable", token);
        genned.add(getStackParsed);
        instructions.add("pushstring");
        HashMap<String, String> pushstring = new HashMap<>();
        pushstring.put("token", token);
        genned.add(pushstring);
        return new CodeSegment(instructions, genned);
    }
}
