package main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrintLnInstruction implements InstructionHandler {
    @Override
    public Map<String, String> run(HashMap<String, Integer> variables, List<String> args) {
        HashMap<String, String> parsed = new HashMap<>();
        parsed.put("variableName", args.get(1));
        return parsed;
    }
}
