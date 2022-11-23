package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyAccessAST extends AST {
    public List<AST> children = new ArrayList<>();
    @Override
    public void add(AST astNode) {
        children.add(astNode);
    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating property access AST %s", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();
        instructions.add("property");
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("variable", ((IdentifierAST) this.children.get(0)).token);
        genned.add(parsed);
        return new CodeSegment(instructions, genned);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("propertyaccess[");
        for (AST child : children) {
            sb.append(child);
        }
        sb.append(" propertyaccess]");
        return sb.toString();
    }
}
