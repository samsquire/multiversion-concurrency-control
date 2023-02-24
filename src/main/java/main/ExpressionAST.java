package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExpressionAST extends AST {
    public List<AST> children;

    public ExpressionAST(AST ast) {
        this.children = new ArrayList<>();
        this.children.add(ast);
    }
    @Override
    public void add(AST astNode) {

        this.children.add(astNode);
    }

    @Override
    public CodeSegment codegen() {
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();
        System.out.println(children);
        ArrayList<AST> list = new ArrayList<>(children);
        Collections.reverse(list);
        for (AST ast : list) {
            CodeSegment codegen = ast.codegen();
            instructions.addAll(codegen.instructions);
            genned.addAll(codegen.parsed);
        }
        return new CodeSegment(instructions, genned);
    }

    public CodeSegment genparameter() {
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();
        ArrayList<AST> list = new ArrayList<>(children);
        for (AST ast : list) {
            if (ast.getClass() == ParameterListAST.class) {
                continue;
            }
            if (ast.getClass() == EndOfParameterListAST.class) {
                continue;
            }
            CodeSegment codegen = ast.genparameter();
            instructions.addAll(codegen.instructions);
            genned.addAll(codegen.parsed);
        }
        return new CodeSegment(instructions, genned);
    }

    public boolean hasOperator() {
        for (AST child : children) {
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (AST child : children) {
            sb.append(String.format("%s ", child));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean containsOperator() {
        for (AST child : children) {
            if (child != null && child.getClass() == OperatorAST.class) {
                return true;
            }
        }
        return false;
    }
}
