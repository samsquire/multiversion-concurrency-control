package main;

public class LiteralNumberAST extends AST {
    private String token;

    public LiteralNumberAST(String token) {
        this.token = token;
    }

    @Override
    public void add(AST astNode) {

    }
}
