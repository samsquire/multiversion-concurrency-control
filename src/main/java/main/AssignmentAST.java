package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentAST extends AST {
    private final DestinationVariable destinationVariable;
    private List<String> operators;
    private final List<String> rhs;

    public AssignmentAST(DestinationVariable destinationVariable, List<String> operators, List<String> rhs) {
        super();
        this.destinationVariable = destinationVariable;
        this.operators = operators;
        this.rhs = rhs;
    }

    public String toString() {
        return String.format("%s %s %s", destinationVariable, operators, rhs);
    }

    @Override
    public void add(AST astNode) {

    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating assignment AST %s", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();
        System.out.println("AST assignment children");
        System.out.println(destinationVariable);
        System.out.println(rhs);
        System.out.println(operators);
        return new CodeSegment(instructions, genned);
    }
}
