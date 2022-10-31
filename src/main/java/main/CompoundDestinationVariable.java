package main;

import java.util.List;

public class CompoundDestinationVariable extends DestinationVariable {
    private final List<String> path;

    public CompoundDestinationVariable(List<String> path) {

        this.path = path;
    }
    public String toString() {
        return path.toString();
    }
}
