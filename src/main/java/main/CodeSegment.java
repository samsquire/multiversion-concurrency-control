package main;

import java.util.List;
import java.util.Map;

public class CodeSegment {
    List<String> instructions;
    List<Map<String, String>> parsed;
    public CodeSegment(List<String> instructions, List<Map<String, String>> parsed) {
        this.instructions = instructions;
        this.parsed = parsed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CODE SEGMENT INSTRUCTIONS\n");
        for (String instruction : this.instructions) {
            sb.append(instruction);
            sb.append("\n");
        }
        sb.append("\n");
        sb.append("CODE SEGMENT PARSED");
        sb.append(this.parsed);
        return sb.toString();
    }
}
