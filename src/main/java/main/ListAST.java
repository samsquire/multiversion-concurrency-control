package main;

import java.util.List;

public class ListAST extends AST {
    private final List<AST> items;

    public ListAST(List<AST> items) {
        super();
        this.items = items;
    }

    @Override
    public void add(AST astNode) {
        this.items.add(astNode);
    }
}
