package main;

public class IdentifierAST extends AST {
    private final String token;

    public IdentifierAST(String token) {
        super();
        this.token = token;
    }

    @Override
    public void add(AST astNode) {

    }
    public String toString() {
        return String.format("Identifier %s",  token);
    }
}
