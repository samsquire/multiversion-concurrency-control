package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperatorAST extends AST {
    private String operator;
    public OperatorAST(String token) {
        super();
        this.operator = token;
    }

    @Override
    public void add(AST astNode) {

    }
    public String toString() {
        return operator;
    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating property access AST %s", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();

        switch (operator) {
            case "pluseq":
                instructions.add("pluseq");
                genned.add(new HashMap<>());
                break;
            case "eq":
                instructions.add("eq");
                genned.add(new HashMap<>());
                break;
            case "minuseq":
                instructions.add("minuseq");
                genned.add(new HashMap<>());
                break;
        }


        return new CodeSegment(instructions, genned);
    }
}
