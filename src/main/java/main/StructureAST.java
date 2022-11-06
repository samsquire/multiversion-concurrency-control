package main;

import java.util.HashMap;
import java.util.Map;

public class StructureAST extends AST {
    private final Map<AST, AST> ast;

    public StructureAST() {
        super();
        this.ast = new HashMap<>();
    }

    @Override
    public void add(AST astNode) {

    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HASH");
        for (Map.Entry<AST, AST> entry : ast.entrySet()) {
            sb.append(String.format("HASH ENTRY %s = %s", entry.getKey()));
        }
        return sb.toString();
    }
}
