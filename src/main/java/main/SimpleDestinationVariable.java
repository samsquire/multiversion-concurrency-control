package main;

import java.util.List;

public class SimpleDestinationVariable extends DestinationVariable {
    private List<String> lhs;

    public SimpleDestinationVariable(List<String> lhs) {

        this.lhs = lhs;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs);

        return sb.toString();
    }
}
