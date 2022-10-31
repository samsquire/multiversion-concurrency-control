package main;

import java.util.List;

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
}
