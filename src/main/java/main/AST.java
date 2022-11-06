package main;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public abstract class AST {

    public boolean stopped;
    public boolean produced;
    public AST parent;
    public boolean valid;

    public List<Map<String, String>> codegen() {
        return asList();
    }

    public abstract void add(AST astNode);

    public void setStopped() {
        AST currentParent = parent;
        while (currentParent != null) {
            System.out.println("Looking for parents");
            currentParent.stopped = true;
            currentParent = currentParent.parent;
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setHasProduced() {
        this.produced = true;
    }

    protected void setParent(AST parent) {
        this.parent = parent;
    }
    public boolean isValid() {
        return valid;
    }
}
