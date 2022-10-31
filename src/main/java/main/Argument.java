package main;

public class Argument {
    private final String name;
    private final String type;

    public Argument(String name, String type) {
        this.name = name;
        this.type = type;
    }
    public String toString() {
        return String.format("argument %s %s", type, name);
    }
}
