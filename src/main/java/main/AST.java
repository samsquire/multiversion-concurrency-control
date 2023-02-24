package main;

import java.util.ArrayList;

public abstract class AST {

    public boolean stopped;
    public boolean produced;
    public AST parent;
    public boolean valid;
    public int depth = 0;
    public String type;

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

    public boolean containsOperator() {
        return false;
    }

    public void setType(String originalType) {
        System.out.println(String.format("Setting type to %s", originalType));
        this.type = originalType;
    }


    protected boolean hasOperator() {
        return false;
    }

    protected CodeSegment genparameter() {
        return null;
    }
}
