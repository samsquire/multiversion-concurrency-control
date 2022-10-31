package main;

public class VariableDeclarationAST extends AST {
    private final String variableType;
    private final String variableName;
    private AST expression;

    public VariableDeclarationAST(String variableType, String variableName, AST expression) {
        this.variableType = variableType;
        this.variableName = variableName;
        this.expression = expression;
    }

    @Override
    public void add(AST astNode) {

    }
    public String toString() {
        return "%s %s = %s".format(variableType, variableName, expression);
    }
}
