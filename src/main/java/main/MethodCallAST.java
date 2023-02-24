package main;

import org.apache.tools.ant.taskdefs.XSLTProcess;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodCallAST extends AST {
    private String methodName;
    private AST parameterList;

    public MethodCallAST(String methodName, AST parameterList) {
        this.methodName = methodName;
        this.parameterList = parameterList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[ MethodCall %s", methodName));
        sb.append(parameterList.toString());
        sb.append(" ]");
        return sb.toString();
    }

    @Override
    public void add(AST astNode) {

    }

    @Override
    public CodeSegment codegen() {
        System.out.println(String.format("Generating method call AST %s", this));
        List<String> instructions = new ArrayList<>();
        List<Map<String, String>> genned = new ArrayList<>();

        System.out.println(String.format("Parameter list is %s", parameterList));

        CodeSegment codesegment = ((ExpressionAST) parameterList).genparameter();
        instructions.addAll(codesegment.instructions);
        genned.addAll(codesegment.parsed);
        instructions.add("call");
        HashMap<String, String> callData = new HashMap<>();
        callData.put("method", methodName);
        genned.add(callData);

        return new CodeSegment(instructions, genned);
    }
}
