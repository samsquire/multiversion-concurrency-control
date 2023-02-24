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

    public boolean hasOperator() {
        for (AST child : items) {
            if (child instanceof OperatorAST) {
                // need to process in reverse order
                return true;
            }
            if (child.hasOperator()) {
                return true;
            }
        }
        return false;
    }
}
