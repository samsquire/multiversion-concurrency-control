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
        boolean variable = false;
        if (name.getClass() == LiteralStringAST.class) {
            variable = false;
            token = ((LiteralStringAST) name).token;
        }
        if (name.getClass() == IdentifierAST.class) {
            token = ((IdentifierAST) name).token;
            variable = true;
        }
        instructions.add("pushstring");

        HashMap<String, String> addParsed = new HashMap<>();
        addParsed.put("token", token);
        addParsed.put("type", "string");
        genned.add(addParsed);
        if (variable) {
            instructions.add("loadhashvar");
        } else {
            instructions.add("loadhash");
        }
        HashMap<String, String> getStackParsed = new HashMap<>();
        addParsed.put("variable", token);
        genned.add(getStackParsed);
        instructions.add("pushstring");
        HashMap<String, String> pushstring = new HashMap<>();
        pushstring.put("token", token);
        genned.add(pushstring);
        return new CodeSegment(instructions, genned);
    }

    @Override
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
