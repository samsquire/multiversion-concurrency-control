package main;

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
}
