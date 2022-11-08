package main;

import java.util.HashMap;
import java.util.Map;

public class StructureAST extends AST {
    private final Map<AST, AST> ast;

    public StructureAST(Map<AST, AST> data) {
        super();
        this.ast = data;
    }

    @Override
    public void add(AST astNode) {

    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HASH");
        for (Map.Entry<AST, AST> entry : ast.entrySet()) {
            sb.append(String.format("ENTRY %s = %s", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}
