package main;

public class EndOfExpressionAST extends AST {
    public EndOfExpressionAST(boolean stopped) {
        this.stopped = stopped;
        this.valid = false;
    }

    @Override
    public void add(AST astNode) {

    }
}
