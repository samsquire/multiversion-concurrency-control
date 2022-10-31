package main;

public class LiteralStringAST extends AST {
    private String token;

    public LiteralStringAST(String token) {
        super();
        this.token = token;
    }

    @Override
    public void add(AST astNode) {

    }
}
