package main;

import java.util.ArrayList;

public abstract class AST {

    public boolean stopped;
    public boolean produced;
    public AST parent;
    public boolean valid;

    public CodeSegment codegen() {
        return new CodeSegment(new ArrayList<>(), new ArrayList<>());
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
