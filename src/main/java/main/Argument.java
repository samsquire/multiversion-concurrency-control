package main;

public class Argument {
    public final String name;
    public final String type;

    public Argument(String name, String type) {
        this.name = name;
        this.type = type;
    }
    public String toString() {
        return String.format("argument %s %s", type, name);
    }
}
